package com.yarithdev.smart_geofence.fgs

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.ForegroundLaunchState
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import org.json.JSONArray
import org.json.JSONObject

data class ForegroundStartSpec(
    val serviceKey: String,
    val logService: String,
    val tag: String,
    val receiverClass: Class<out BroadcastReceiver>,
    val startRequestCode: Int,
    val watchdogRequestCode: Int,
    val startAttemptExtra: String,
    val launchTokenExtra: String,
    val alarmPurposeExtra: String,
    val purposeStart: String,
    val purposeWatchdog: String,
)

data class QueuedForegroundStart(
    val serviceKey: String,
    val attempt: Int,
    val launchToken: Long,
    val reason: String,
)

object ForegroundStartCoordinator {
    private const val TAG = "ForegroundStartCoordinator"
    private const val BATCH_LOG_SERVICE = "fgs_batch"
    private const val KEY_BATCH_QUEUE = "foreground_service_batch_queue"
    private const val KEY_BATCH_WINDOW_CLOSED = "foreground_service_batch_window_closed"
    private const val KEY_BATCH_EXPECTED = "foreground_service_batch_expected"
    private const val KEY_BATCH_READY = "foreground_service_batch_ready"

    fun ensureLaunchToken(context: Context, spec: ForegroundStartSpec, reason: String): Long =
        ForegroundLaunchState.ensureToken(context.applicationContext, spec.serviceKey, reason)

    fun beginLaunch(context: Context, spec: ForegroundStartSpec, reason: String): Long =
        ForegroundLaunchState.begin(context.applicationContext, spec.serviceKey, reason)

    fun isCurrent(context: Context, spec: ForegroundStartSpec, token: Long): Boolean =
        ForegroundLaunchState.isCurrent(context.applicationContext, spec.serviceKey, token)

    fun markFailure(
        context: Context,
        spec: ForegroundStartSpec,
        token: Long,
        reason: String,
    ) {
        ForegroundLaunchState.markFailure(context.applicationContext, spec.serviceKey, token, reason)
    }

    fun markStopped(context: Context, spec: ForegroundStartSpec) {
        ForegroundLaunchState.markStopped(context.applicationContext, spec.serviceKey)
    }

    fun onForegroundReady(
        context: Context,
        spec: ForegroundStartSpec,
        attempt: Int,
        launchToken: Long,
    ): Boolean {
        val appContext = context.applicationContext
        val snapshot = ForegroundLaunchState.markReady(appContext, spec.serviceKey, launchToken)
        if (snapshot == null) {
            SmartGeofenceLogger.fgs(appContext, spec.logService, "foreground_ready_ignored_stale", launchToken, attempt)
            SmartGeofenceLogger.d(appContext, spec.tag, "Ignoring foreground-ready for stale token=$launchToken attempt=$attempt.")
            return false
        }
        cancelWatchdog(appContext, spec)
        val latencyMs = (snapshot.foregroundReadyAtMillis - snapshot.launchRequestedAtMillis)
            .takeIf { it >= 0L }
        SmartGeofenceLogger.fgs(
            appContext,
            spec.logService,
            "foreground_ready",
            launchToken,
            attempt,
            "latency=${latencyMs}ms"
        )
        SmartGeofenceLogger.d(
            appContext,
            spec.tag,
            "Foreground service ready token=$launchToken attempt=$attempt latency=${latencyMs}ms."
        )
        markBatchServiceFinished(
            appContext,
            spec.serviceKey,
            "foreground_ready",
            spec.logService,
            launchToken,
            attempt
        )
        return true
    }

    fun onForegroundStartFailed(
        context: Context,
        spec: ForegroundStartSpec,
        attempt: Int,
        launchToken: Long,
        reason: String,
    ) {
        val appContext = context.applicationContext
        ForegroundLaunchState.markFailure(appContext, spec.serviceKey, launchToken, reason)
        cancelWatchdog(appContext, spec)
        SmartGeofenceLogger.fgsWarning(
            appContext,
            spec.logService,
            "foreground_failed",
            launchToken,
            attempt,
            "reason=$reason"
        )
        SmartGeofenceLogger.w(
            appContext,
            spec.tag,
            "Foreground start failed token=$launchToken attempt=$attempt reason=$reason."
        )
        markBatchServiceFinished(
            appContext,
            spec.serviceKey,
            "foreground_failed:$reason",
            spec.logService,
            launchToken,
            attempt
        )
    }

    fun queueStart(
        context: Context,
        spec: ForegroundStartSpec,
        delayMillis: Long,
        attempt: Int,
        launchToken: Long,
        reason: String,
    ): Boolean {
        val appContext = context.applicationContext
        if (!isCurrent(appContext, spec, launchToken)) {
            SmartGeofenceLogger.fgsWarning(
                appContext,
                spec.logService,
                "batch_queue_skipped_stale",
                launchToken,
                attempt
            )
            SmartGeofenceLogger.d(
                appContext,
                spec.tag,
                "Batch queue skipped; stale token=$launchToken attempt=$attempt."
            )
            return false
        }

        val request = QueuedForegroundStart(
            serviceKey = spec.serviceKey,
            attempt = attempt,
            launchToken = launchToken,
            reason = reason,
        )
        val queueSize = enqueueBatchLaunch(appContext, request)
        closeDeliveryWindow(appContext, "queued ${spec.serviceKey}: $reason")
        SmartGeofenceLogger.fgs(
            appContext,
            spec.logService,
            "batch_queue_joined",
            launchToken,
            attempt,
            "delay=${delayMillis.coerceAtLeast(0L)}ms queueSize=$queueSize reason=$reason"
        )

        val normalizedDelay = delayMillis.coerceAtLeast(0L)
        if (normalizedDelay == 0L) {
            ForegroundServiceLaunchReceiver.drain(appContext)
            return true
        }
        if (batchStartPendingIntentExists(appContext)) {
            SmartGeofenceLogger.fgs(
                appContext,
                BATCH_LOG_SERVICE,
                "batch_start_already_pending",
                launchToken,
                attempt,
                "joinedService=${spec.serviceKey} queueSize=$queueSize"
            )
            return true
        }
        return scheduleBatchStartAlarm(appContext, normalizedDelay, launchToken, attempt, reason)
    }

    fun scheduleStartAlarm(
        context: Context,
        spec: ForegroundStartSpec,
        delayMillis: Long,
        attempt: Int,
        launchToken: Long,
        reason: String,
        stage: String = "start_alarm",
    ): Boolean {
        val appContext = context.applicationContext
        if (!isCurrent(appContext, spec, launchToken)) {
            SmartGeofenceLogger.fgs(appContext, spec.logService, "${stage}_skipped_stale", launchToken, attempt)
            SmartGeofenceLogger.d(appContext, spec.tag, "Start alarm skipped; stale token=$launchToken attempt=$attempt.")
            return false
        }
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            markFailure(appContext, spec, launchToken, "AlarmManager unavailable")
            SmartGeofenceLogger.fgsWarning(
                appContext,
                spec.logService,
                "${stage}_failed",
                launchToken,
                attempt,
                "reason=alarm_manager_unavailable"
            )
            SmartGeofenceLogger.w(appContext, spec.tag, "AlarmManager unavailable; cannot schedule foreground-service start.")
            return false
        }
        val normalizedDelay = delayMillis.coerceAtLeast(0L)
        val triggerAt = System.currentTimeMillis() + normalizedDelay
        val pending = pendingIntent(
            appContext,
            spec,
            PendingIntent.FLAG_UPDATE_CURRENT,
            spec.startRequestCode,
            spec.purposeStart,
            attempt,
            launchToken
        )
        return try {
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            when {
                exactAllowed -> alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            SmartGeofenceLogger.fgs(
                appContext,
                spec.logService,
                if (exactAllowed) "${stage}_scheduled" else "${stage}_inexact_scheduled",
                launchToken,
                attempt,
                "delay=${normalizedDelay}ms triggerAt=$triggerAt reason=$reason exact=$exactAllowed"
            )
            SmartGeofenceLogger.d(
                appContext,
                spec.tag,
                "Scheduled foreground-service start alarm token=$launchToken attempt=$attempt " +
                    "delay=${normalizedDelay}ms triggerAt=$triggerAt reason=$reason exact=$exactAllowed."
            )
            true
        } catch (e: SecurityException) {
            pending.cancel()
            markFailure(appContext, spec, launchToken, "start alarm SecurityException")
            SmartGeofenceLogger.fgsWarning(
                appContext,
                spec.logService,
                "${stage}_failed",
                launchToken,
                attempt,
                "reason=security_exception",
                e
            )
            SmartGeofenceLogger.w(appContext, spec.tag, "Failed to schedule start alarm: ${e.message}", e)
            false
        }
    }

    fun scheduleWatchdog(
        context: Context,
        spec: ForegroundStartSpec,
        launchToken: Long,
        attempt: Int,
        timeoutMillis: Long,
    ): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            SmartGeofenceLogger.w(appContext, spec.tag, "AlarmManager unavailable; cannot schedule foreground-ready watchdog.")
            return false
        }
        val normalizedTimeout = timeoutMillis.coerceAtLeast(1_000L)
        val triggerAt = System.currentTimeMillis() + normalizedTimeout
        val pending = pendingIntent(
            appContext,
            spec,
            PendingIntent.FLAG_UPDATE_CURRENT,
            spec.watchdogRequestCode,
            spec.purposeWatchdog,
            attempt,
            launchToken
        )
        return try {
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            SmartGeofenceLogger.fgs(
                appContext,
                spec.logService,
                if (exactAllowed) "watchdog_scheduled" else "watchdog_inexact_scheduled",
                launchToken,
                attempt,
                "timeout=${normalizedTimeout}ms triggerAt=$triggerAt exact=$exactAllowed"
            )
            SmartGeofenceLogger.d(
                appContext,
                spec.tag,
                "Scheduled foreground-ready watchdog token=$launchToken attempt=$attempt " +
                    "timeout=${normalizedTimeout}ms exact=$exactAllowed."
            )
            true
        } catch (e: SecurityException) {
            pending.cancel()
            SmartGeofenceLogger.w(appContext, spec.tag, "Failed to schedule watchdog: ${e.message}", e)
            false
        }
    }

    fun cancelStart(context: Context, spec: ForegroundStartSpec) {
        cancelPendingIntent(context.applicationContext, spec, spec.startRequestCode, spec.purposeStart)
    }

    fun cancelWatchdog(context: Context, spec: ForegroundStartSpec) {
        val appContext = context.applicationContext
        existingPendingIntent(appContext, spec, spec.watchdogRequestCode, spec.purposeWatchdog)?.let {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(it)
            it.cancel()
            SmartGeofenceLogger.fgs(
                appContext,
                spec.logService,
                "watchdog_cancelled",
                ForegroundLaunchState.snapshot(appContext, spec.serviceKey).activeToken
            )
            SmartGeofenceLogger.d(appContext, spec.tag, "Cancelled foreground-ready watchdog.")
        }
    }

    fun startPendingIntentExists(context: Context, spec: ForegroundStartSpec): Boolean =
        existingPendingIntent(context.applicationContext, spec, spec.startRequestCode, spec.purposeStart) != null

    fun watchdogPendingIntentExists(context: Context, spec: ForegroundStartSpec): Boolean =
        existingPendingIntent(context.applicationContext, spec, spec.watchdogRequestCode, spec.purposeWatchdog) != null

    fun batchStartPendingIntentExists(context: Context): Boolean =
        existingBatchPendingIntent(context.applicationContext) != null

    fun batchWindowClosed(context: Context): Boolean =
        prefs(context.applicationContext).getBoolean(KEY_BATCH_WINDOW_CLOSED, false)

    fun queuedServiceKeys(context: Context): List<String> =
        loadBatchQueue(context.applicationContext).map { it.serviceKey }

    @Synchronized
    fun removeQueuedStart(context: Context, spec: ForegroundStartSpec) {
        val appContext = context.applicationContext
        val remaining = loadBatchQueue(appContext)
            .filterNot { it.serviceKey == spec.serviceKey }
        persistBatchQueue(appContext, remaining)
        if (remaining.isEmpty()) {
            cancelBatchStart(appContext)
            openDeliveryWindow(appContext, "queue_empty_after_remove")
        }
    }

    @Synchronized
    fun drainQueuedStarts(context: Context): List<QueuedForegroundStart> {
        val appContext = context.applicationContext
        cancelBatchStart(appContext)
        val queued = loadBatchQueue(appContext)
        persistBatchQueue(appContext, emptyList())
        val expected = queued.map { it.serviceKey }.toSet()
        prefs(appContext).edit()
            .putStringSet(KEY_BATCH_EXPECTED, expected)
            .putStringSet(KEY_BATCH_READY, emptySet())
            .apply()

        SmartGeofenceLogger.fgs(
            appContext,
            BATCH_LOG_SERVICE,
            "batch_drain",
            0L,
            detail = "count=${queued.size} services=${expected.joinToString(",")}"
        )
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Draining foreground-service start batch count=${queued.size} services=$expected."
        )
        if (queued.isEmpty()) {
            openDeliveryWindow(appContext, "empty_batch")
        }
        return queued
    }

    @Synchronized
    fun markBatchServiceFinished(
        context: Context,
        serviceKey: String,
        reason: String,
        logService: String = BATCH_LOG_SERVICE,
        launchToken: Long = 0L,
        attempt: Int = 0,
    ) {
        val appContext = context.applicationContext
        val prefs = prefs(appContext)
        val expected = prefs.getStringSet(KEY_BATCH_EXPECTED, emptySet()).orEmpty()
        if (!prefs.getBoolean(KEY_BATCH_WINDOW_CLOSED, false) || !expected.contains(serviceKey)) {
            return
        }
        val ready = prefs.getStringSet(KEY_BATCH_READY, emptySet()).orEmpty().toMutableSet()
        ready.add(serviceKey)
        prefs.edit()
            .putStringSet(KEY_BATCH_READY, ready)
            .apply()
        SmartGeofenceLogger.fgs(
            appContext,
            logService,
            "batch_service_finished",
            launchToken,
            attempt,
            "service=$serviceKey reason=$reason ready=${ready.size}/${expected.size}"
        )
        if (ready.containsAll(expected)) {
            openDeliveryWindow(appContext, "all_services_finished")
        }
    }

    private fun cancelPendingIntent(
        context: Context,
        spec: ForegroundStartSpec,
        requestCode: Int,
        purpose: String,
    ) {
        existingPendingIntent(context, spec, requestCode, purpose)?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(it)
            it.cancel()
            SmartGeofenceLogger.d(context, spec.tag, "Cancelled foreground-service $purpose alarm.")
        }
    }

    private fun existingPendingIntent(
        context: Context,
        spec: ForegroundStartSpec,
        requestCode: Int,
        purpose: String,
    ): PendingIntent? =
        pendingIntent(context, spec, PendingIntent.FLAG_NO_CREATE, requestCode, purpose)

    private fun pendingIntent(
        context: Context,
        spec: ForegroundStartSpec,
        baseFlags: Int,
        requestCode: Int,
        purpose: String,
        attempt: Int? = null,
        launchToken: Long? = null,
    ): PendingIntent {
        val intent = Intent(context, spec.receiverClass).apply {
            putExtra(spec.alarmPurposeExtra, purpose)
            if (attempt != null) putExtra(spec.startAttemptExtra, attempt)
            if (launchToken != null) putExtra(spec.launchTokenExtra, launchToken)
        }
        var flags = baseFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun scheduleBatchStartAlarm(
        context: Context,
        delayMillis: Long,
        launchToken: Long,
        attempt: Int,
        reason: String,
    ): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            SmartGeofenceLogger.fgsWarning(
                appContext,
                BATCH_LOG_SERVICE,
                "batch_start_failed",
                launchToken,
                attempt,
                "reason=alarm_manager_unavailable"
            )
            SmartGeofenceLogger.w(appContext, TAG, "AlarmManager unavailable; cannot schedule FGS batch.")
            return false
        }
        val triggerAt = System.currentTimeMillis() + delayMillis
        val pending = batchPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT)
        return try {
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            when {
                exactAllowed -> alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            SmartGeofenceLogger.fgs(
                appContext,
                BATCH_LOG_SERVICE,
                if (exactAllowed) "batch_start_scheduled" else "batch_start_inexact_scheduled",
                launchToken,
                attempt,
                "delay=${delayMillis}ms triggerAt=$triggerAt reason=$reason exact=$exactAllowed"
            )
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Scheduled FGS batch start delay=${delayMillis}ms triggerAt=$triggerAt " +
                    "reason=$reason exact=$exactAllowed."
            )
            true
        } catch (e: SecurityException) {
            pending.cancel()
            SmartGeofenceLogger.fgsWarning(
                appContext,
                BATCH_LOG_SERVICE,
                "batch_start_failed",
                launchToken,
                attempt,
                "reason=security_exception",
                e
            )
            SmartGeofenceLogger.w(appContext, TAG, "Failed to schedule FGS batch start: ${e.message}", e)
            false
        }
    }

    private fun cancelBatchStart(context: Context) {
        existingBatchPendingIntent(context.applicationContext)?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(it)
            it.cancel()
        }
    }

    private fun existingBatchPendingIntent(context: Context): PendingIntent? =
        batchPendingIntent(context, PendingIntent.FLAG_NO_CREATE)

    private fun batchPendingIntent(context: Context, baseFlags: Int): PendingIntent {
        val intent = Intent(context, ForegroundServiceLaunchReceiver::class.java)
        var flags = baseFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_FGS_BATCH_LAUNCH,
            intent,
            flags
        )
    }

    @Synchronized
    private fun enqueueBatchLaunch(
        context: Context,
        request: QueuedForegroundStart,
    ): Int {
        val queued = loadBatchQueue(context).associateBy { it.serviceKey }.toMutableMap()
        queued[request.serviceKey] = request
        persistBatchQueue(context, queued.values)
        return queued.size
    }

    private fun loadBatchQueue(context: Context): List<QueuedForegroundStart> {
        val raw = prefs(context).getString(KEY_BATCH_QUEUE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { index ->
                val item = arr.optJSONObject(index) ?: return@mapNotNull null
                val serviceKey = item.optString("serviceKey").takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val token = item.optLong("launchToken", 0L)
                if (token <= 0L) return@mapNotNull null
                QueuedForegroundStart(
                    serviceKey = serviceKey,
                    attempt = item.optInt("attempt", 0),
                    launchToken = token,
                    reason = item.optString("reason", "queued"),
                )
            }
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(context, TAG, "Failed to parse FGS batch queue: ${e.message}", e)
            emptyList()
        }
    }

    private fun persistBatchQueue(
        context: Context,
        queued: Collection<QueuedForegroundStart>,
    ) {
        val arr = JSONArray()
        queued.forEach { request ->
            arr.put(
                JSONObject()
                    .put("serviceKey", request.serviceKey)
                    .put("attempt", request.attempt)
                    .put("launchToken", request.launchToken)
                    .put("reason", request.reason)
            )
        }
        prefs(context).edit()
            .putString(KEY_BATCH_QUEUE, arr.toString())
            .apply()
    }

    private fun closeDeliveryWindow(context: Context, reason: String) {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_BATCH_WINDOW_CLOSED, false)) {
            prefs.edit()
                .putBoolean(KEY_BATCH_WINDOW_CLOSED, true)
                .putStringSet(KEY_BATCH_EXPECTED, emptySet())
                .putStringSet(KEY_BATCH_READY, emptySet())
                .apply()
            SmartGeofenceLogger.fgs(
                context,
                BATCH_LOG_SERVICE,
                "delivery_paused",
                0L,
                detail = "reason=$reason"
            )
            SmartGeofenceLogger.d(context, TAG, "Foreground-service delivery window closed: $reason.")
        }
    }

    private fun openDeliveryWindow(context: Context, reason: String) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_BATCH_WINDOW_CLOSED, false)) {
            prefs.edit()
                .putBoolean(KEY_BATCH_WINDOW_CLOSED, false)
                .putStringSet(KEY_BATCH_EXPECTED, emptySet())
                .putStringSet(KEY_BATCH_READY, emptySet())
                .apply()
            SmartGeofenceLogger.fgs(
                context,
                BATCH_LOG_SERVICE,
                "delivery_resumed",
                0L,
                detail = "reason=$reason"
            )
            SmartGeofenceLogger.d(context, TAG, "Foreground-service delivery window opened: $reason.")
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
}

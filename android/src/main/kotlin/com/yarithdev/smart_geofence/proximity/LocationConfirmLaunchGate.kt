package com.yarithdev.smart_geofence.proximity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.ForegroundLaunchState
import com.yarithdev.smart_geofence.SmartGeofenceFence
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.fgs.ForegroundStartCoordinator
import com.yarithdev.smart_geofence.fgs.ForegroundStartSpec

object LocationConfirmLaunchGate {
    private const val TAG = "LocationConfirmLaunchGate"
    private const val FGS_LOG_SERVICE = "confirm"
    const val EXTRA_START_ATTEMPT = "smart_geofence.location_confirm_start_attempt"
    const val EXTRA_LAUNCH_TOKEN = "smart_geofence.location_confirm_launch_token"
    const val EXTRA_ALARM_PURPOSE = "smart_geofence.location_confirm_alarm_purpose"
    const val PURPOSE_START = "start"
    const val PURPOSE_WATCHDOG = "watchdog"
    private val RETRY_DELAYS_MS = longArrayOf(250L, 2_000L, 15_000L, 45_000L, 120_000L)
    private val LAUNCH_SPEC = ForegroundStartSpec(
        serviceKey = ForegroundLaunchState.SERVICE_LOCATION_CONFIRM,
        logService = FGS_LOG_SERVICE,
        tag = TAG,
        receiverClass = LocationConfirmLaunchReceiver::class.java,
        startRequestCode = Constants.PENDING_INTENT_REQUEST_CONFIRM_LAUNCH,
        watchdogRequestCode = Constants.PENDING_INTENT_REQUEST_CONFIRM_WATCHDOG,
        startAttemptExtra = EXTRA_START_ATTEMPT,
        launchTokenExtra = EXTRA_LAUNCH_TOKEN,
        alarmPurposeExtra = EXTRA_ALARM_PURPOSE,
        purposeStart = PURPOSE_START,
        purposeWatchdog = PURPOSE_WATCHDOG,
    )

    fun enqueue(context: Context, fence: SmartGeofenceFence, source: String): Boolean {
        val appContext = context.applicationContext
        val request = LocationConfirmQueue.enqueue(appContext, fence.id, source)
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Launch requested for confirm id=${request.id} fence=${fence.id} source=$source."
        )
        return launchQueuedRequest(
            appContext,
            tokenReason = "queued confirm id=${request.id}",
            logDetail = "requestId=${request.id} fence=${fence.id} source=$source",
        )
    }

    fun enqueueNearest(context: Context, source: String): Boolean {
        val appContext = context.applicationContext
        val request = LocationConfirmQueue.enqueueNearest(appContext, source)
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Launch requested for nearest confirm id=${request.id} source=$source."
        )
        return launchQueuedRequest(
            appContext,
            tokenReason = "queued nearest confirm id=${request.id}",
            logDetail = "requestId=${request.id} source=$source nearest=true",
        )
    }

    private fun launchQueuedRequest(
        context: Context,
        tokenReason: String,
        logDetail: String,
    ): Boolean {
        val appContext = context.applicationContext
        val reuseLaunch = (
            LocationConfirmService.isRunning ||
                pendingIntentExists(appContext) ||
                watchdogPendingIntentExists(appContext)
            )
        val token = if (reuseLaunch) {
            ForegroundStartCoordinator.ensureLaunchToken(
                appContext,
                LAUNCH_SPEC,
                tokenReason
            )
        } else {
            ForegroundStartCoordinator.beginLaunch(
                appContext,
                LAUNCH_SPEC,
                tokenReason
            )
        }
        SmartGeofenceLogger.fgs(
            appContext,
            FGS_LOG_SERVICE,
            "launch_requested",
            token,
            0,
            "$logDetail reused=$reuseLaunch"
        )
        if (reuseLaunch) {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirm launch already running or scheduled; queued request will drain token=$token."
            )
            return true
        }

        val delayMs = SmartGeofenceConfigStore.load(appContext)
            .foregroundServiceStartDelayMillis
            .coerceAtLeast(0L)
        if (delayMs == 0L) {
            return requestStart(appContext, attempt = 0, reason = "queued", launchToken = token)
        }

        val scheduled = ForegroundStartCoordinator.queueStart(
            appContext,
            LAUNCH_SPEC,
            delayMs,
            attempt = 0,
            launchToken = token,
            reason = "queued"
        )
        if (scheduled) return true

        SmartGeofenceLogger.w(
            appContext,
            TAG,
            "Delayed confirm start alarm was not scheduled; trying immediate foreground-service start."
        )
        return requestStart(appContext, attempt = 0, reason = "queued alarm fallback", launchToken = token)
    }

    fun requestStart(
        context: Context,
        attempt: Int,
        reason: String,
        launchToken: Long = 0L,
    ): Boolean {
        val appContext = context.applicationContext
        val token = if (launchToken > 0L) {
            launchToken
        } else {
            ForegroundStartCoordinator.ensureLaunchToken(
                appContext,
                LAUNCH_SPEC,
                reason
            )
        }
        if (!ForegroundStartCoordinator.isCurrent(appContext, LAUNCH_SPEC, token)) {
            SmartGeofenceLogger.fgsWarning(
                appContext,
                FGS_LOG_SERVICE,
                "start_skipped_stale",
                token,
                attempt
            )
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirm service start skipped; stale launch token=$token attempt=$attempt."
            )
            return false
        }
        ForegroundStartCoordinator.cancelStart(appContext, LAUNCH_SPEC)
        val pendingCount = LocationConfirmQueue.count(appContext)
        if (pendingCount == 0) {
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "start_skipped",
                token,
                attempt,
                "reason=empty_queue"
            )
            SmartGeofenceLogger.d(appContext, TAG, "Confirm service start skipped; queue is empty.")
            return false
        }
        if (!canStartForegroundLocationService(appContext)) {
            ForegroundStartCoordinator.markFailure(
                appContext,
                LAUNCH_SPEC,
                token,
                "foreground-location service prerequisites missing"
            )
            SmartGeofenceLogger.fgsWarning(
                appContext,
                FGS_LOG_SERVICE,
                "start_skipped",
                token,
                attempt,
                "reason=missing_prerequisites"
            )
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Confirm service start skipped; foreground-location service prerequisites are missing."
            )
            return false
        }
        return try {
            val intent = Intent(appContext, LocationConfirmService::class.java)
                .putExtra(EXTRA_START_ATTEMPT, attempt)
                .putExtra(EXTRA_LAUNCH_TOKEN, token)
            ContextCompat.startForegroundService(appContext, intent)
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "startForegroundService_accepted",
                token,
                attempt,
                "reason=$reason pending=$pendingCount"
            )
            scheduleWatchdog(appContext, token, attempt)
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Requested confirm foreground service start token=$token attempt=$attempt " +
                    "reason=$reason pending=$pendingCount."
            )
            true
        } catch (e: Throwable) {
            ForegroundStartCoordinator.markFailure(
                appContext,
                LAUNCH_SPEC,
                token,
                "startForegroundService ${e.javaClass.simpleName}"
            )
            SmartGeofenceLogger.fgsWarning(
                appContext,
                FGS_LOG_SERVICE,
                "startForegroundService_failed",
                token,
                attempt,
                "reason=$reason error=${e.javaClass.simpleName}: ${e.message}",
                e
            )
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Confirm foreground service start failed token=$token attempt=$attempt reason=$reason: " +
                    "${e.javaClass.simpleName}: ${e.message}",
                e
            )
            if (LocationConfirmService.isForegroundReady) {
                SmartGeofenceLogger.d(
                    appContext,
                    TAG,
                    "Confirm service is already foreground-ready; queued request will drain there."
                )
                true
            } else {
                if (LocationConfirmService.isRunning) {
                    SmartGeofenceLogger.w(
                        appContext,
                        TAG,
                        "Confirm service exists but is not foreground-ready; scheduling retry."
                    )
                }
                scheduleRetry(
                    appContext,
                    attempt,
                    "startForegroundService ${e.javaClass.simpleName}",
                    token
                )
            }
        }
    }

    fun scheduleRetry(
        context: Context,
        failedAttempt: Int,
        reason: String,
        launchToken: Long = 0L,
    ): Boolean {
        val appContext = context.applicationContext
        val token = if (launchToken > 0L) {
            launchToken
        } else {
            ForegroundStartCoordinator.ensureLaunchToken(
                appContext,
                LAUNCH_SPEC,
                reason
            )
        }
        if (!ForegroundStartCoordinator.isCurrent(appContext, LAUNCH_SPEC, token)) {
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "retry_skipped_stale",
                token,
                failedAttempt
            )
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirm retry skipped; stale launch token=$token."
            )
            return false
        }
        if (LocationConfirmQueue.count(appContext) == 0) {
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "retry_skipped",
                token,
                failedAttempt,
                "reason=empty_queue"
            )
            SmartGeofenceLogger.d(appContext, TAG, "Confirm retry skipped; queue is empty.")
            return false
        }
        val retryDelays = SmartGeofenceConfigStore.load(appContext)
            .foregroundServiceRetryDelaysMillis
            .ifEmpty { RETRY_DELAYS_MS.toList() }
        if (failedAttempt >= retryDelays.size) {
            ForegroundStartCoordinator.markFailure(
                appContext,
                LAUNCH_SPEC,
                token,
                "retry limit reached after $failedAttempt failures; last failure=$reason"
            )
            SmartGeofenceLogger.fgsWarning(
                appContext,
                FGS_LOG_SERVICE,
                "retry_give_up",
                token,
                failedAttempt,
                "lastFailure=$reason"
            )
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Confirm service retry limit reached token=$token after $failedAttempt failures; " +
                    "last failure=$reason."
            )
            return false
        }
        if (!hasReceiver(appContext, LocationConfirmLaunchReceiver::class.java)) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Confirm retry skipped; LocationConfirmLaunchReceiver is not declared."
            )
            return false
        }
        val delayMs = retryDelays[failedAttempt]
        val nextAttempt = failedAttempt + 1
        return scheduleStartAlarm(
            appContext,
            delayMs,
            attempt = nextAttempt,
            launchToken = token,
            reason = reason,
            stage = "retry"
        )
    }

    fun onForegroundReady(context: Context, attempt: Int, launchToken: Long) {
        val appContext = context.applicationContext
        ForegroundStartCoordinator.onForegroundReady(appContext, LAUNCH_SPEC, attempt, launchToken)
    }

    fun onForegroundStartFailed(
        context: Context,
        attempt: Int,
        launchToken: Long,
        reason: String,
    ) {
        ForegroundStartCoordinator.onForegroundStartFailed(
            context.applicationContext,
            LAUNCH_SPEC,
            attempt,
            launchToken,
            reason
        )
    }

    fun onServiceStopped(context: Context) {
        ForegroundStartCoordinator.markStopped(context.applicationContext, LAUNCH_SPEC)
    }

    fun handleWatchdog(context: Context, attempt: Int, launchToken: Long) {
        val appContext = context.applicationContext
        if (!ForegroundStartCoordinator.isCurrent(appContext, LAUNCH_SPEC, launchToken)) {
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "watchdog_ignored_stale",
                launchToken,
                attempt
            )
            SmartGeofenceLogger.d(appContext, TAG, "Confirm watchdog ignored stale token=$launchToken.")
            return
        }
        ForegroundStartCoordinator.cancelWatchdog(appContext, LAUNCH_SPEC)
        if (LocationConfirmService.isForegroundReady) {
            SmartGeofenceLogger.fgs(
                appContext,
                FGS_LOG_SERVICE,
                "watchdog_satisfied",
                launchToken,
                attempt
            )
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirm watchdog satisfied; service is foreground-ready token=$launchToken."
            )
            return
        }
        val reason = "foreground readiness timeout"
        ForegroundStartCoordinator.markFailure(
            appContext,
            LAUNCH_SPEC,
            launchToken,
            reason
        )
        SmartGeofenceLogger.fgsWarning(
            appContext,
            FGS_LOG_SERVICE,
            "watchdog_timeout",
            launchToken,
            attempt,
            "reason=$reason"
        )
        SmartGeofenceLogger.w(
            appContext,
            TAG,
            "Confirm service did not become foreground-ready token=$launchToken attempt=$attempt; retrying."
        )
        try {
            appContext.stopService(Intent(appContext, LocationConfirmService::class.java))
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(appContext, TAG, "Could not stop unready confirm service: ${e.message}", e)
        }
        scheduleRetry(appContext, attempt, reason, launchToken)
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        ForegroundStartCoordinator.cancelStart(appContext, LAUNCH_SPEC)
        ForegroundStartCoordinator.cancelWatchdog(appContext, LAUNCH_SPEC)
        ForegroundStartCoordinator.removeQueuedStart(appContext, LAUNCH_SPEC)
        ForegroundStartCoordinator.markStopped(appContext, LAUNCH_SPEC)
        LocationConfirmQueue.clear(appContext)
        try {
            appContext.stopService(Intent(appContext, LocationConfirmService::class.java))
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(appContext, TAG, "Could not stop confirm service: ${e.message}", e)
        }
    }

    fun pendingIntentExists(context: Context): Boolean =
        ForegroundStartCoordinator.startPendingIntentExists(
            context.applicationContext,
            LAUNCH_SPEC
        ) || ForegroundStartCoordinator.batchStartPendingIntentExists(context.applicationContext)

    fun watchdogPendingIntentExists(context: Context): Boolean =
        ForegroundStartCoordinator.watchdogPendingIntentExists(context.applicationContext, LAUNCH_SPEC)

    fun ensureLaunchToken(context: Context, reason: String): Long =
        ForegroundStartCoordinator.ensureLaunchToken(
            context.applicationContext,
            LAUNCH_SPEC,
            reason
        )

    private fun scheduleStartAlarm(
        context: Context,
        delayMillis: Long,
        attempt: Int,
        launchToken: Long,
        reason: String,
        stage: String = "start_alarm",
    ): Boolean {
        val appContext = context.applicationContext
        if (!hasReceiver(appContext, LocationConfirmLaunchReceiver::class.java)) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Confirm start alarm skipped; LocationConfirmLaunchReceiver is not declared."
            )
            return false
        }
        return ForegroundStartCoordinator.scheduleStartAlarm(
            appContext,
            LAUNCH_SPEC,
            delayMillis,
            attempt,
            launchToken,
            reason,
            stage
        )
    }

    private fun scheduleWatchdog(context: Context, launchToken: Long, attempt: Int): Boolean {
        val timeoutMs = SmartGeofenceConfigStore.load(context.applicationContext)
            .foregroundServiceLaunchTimeoutMillis
            .coerceAtLeast(1_000L)
        return ForegroundStartCoordinator.scheduleWatchdog(
            context.applicationContext,
            LAUNCH_SPEC,
            launchToken,
            attempt,
            timeoutMs
        )
    }

    private fun canStartForegroundLocationService(context: Context): Boolean {
        if (!granted(context, Manifest.permission.FOREGROUND_SERVICE)) {
            SmartGeofenceLogger.w(context, TAG, "FOREGROUND_SERVICE is not declared.")
            return false
        }
        if (Build.VERSION.SDK_INT >= 34 &&
            !granted(context, "android.permission.FOREGROUND_SERVICE_LOCATION")
        ) {
            SmartGeofenceLogger.w(context, TAG, "FOREGROUND_SERVICE_LOCATION is not declared.")
            return false
        }
        val serviceInfo = serviceInfo(context, LocationConfirmService::class.java)
        if (serviceInfo == null) {
            SmartGeofenceLogger.w(context, TAG, "LocationConfirmService is not declared.")
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            (serviceInfo.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) == 0
        ) {
            SmartGeofenceLogger.w(
                context,
                TAG,
                "LocationConfirmService is missing foregroundServiceType=\"location\"."
            )
            return false
        }
        return true
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasReceiver(context: Context, receiverClass: Class<*>): Boolean {
        return try {
            val component = ComponentName(context, receiverClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getReceiverInfo(
                    component,
                    PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getReceiverInfo(component, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun serviceInfo(context: Context, serviceClass: Class<*>): ServiceInfo? {
        return try {
            val component = ComponentName(context, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getServiceInfo(
                    component,
                    PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getServiceInfo(component, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}

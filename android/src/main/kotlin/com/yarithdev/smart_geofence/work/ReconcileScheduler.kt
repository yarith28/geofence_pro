package com.yarithdev.smart_geofence.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceConfig
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic reconcile watchdog.
 *
 * WorkManager is the default and safest scheduler. Alarm mode is an advanced
 * opt-in for apps that want AlarmManager wakeups; it uses exact alarms when
 * permitted and inexact AlarmManager alarms otherwise.
 */
object ReconcileScheduler {
    private const val TAG = "ReconcileScheduler"

    fun schedule(context: Context, config: SmartGeofenceConfig) {
        val appContext = context.applicationContext
        if (config.reconcileScheduler == Constants.RECONCILE_SCHEDULER_EXACT_ALARM) {
            if (scheduleAlarm(appContext, config.reconcileInitialDelayMillis)) {
                cancelWorkManager(appContext)
                SmartGeofenceLogger.d(appContext, TAG, "AlarmManager reconcile scheduled.")
                return
            }
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "AlarmManager reconcile unavailable; falling back to WorkManager."
            )
        } else {
            cancelAlarm(appContext)
        }
        scheduleWorkManager(appContext, config)
    }

    fun scheduleNextAlarm(context: Context, config: SmartGeofenceConfig): Boolean {
        if (config.reconcileScheduler != Constants.RECONCILE_SCHEDULER_EXACT_ALARM) return false
        return scheduleAlarm(
            context.applicationContext,
            TimeUnit.MINUTES.toMillis(config.reconcileIntervalMinutes.coerceAtLeast(1L))
        )
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        cancelWorkManager(appContext)
        cancelAlarm(appContext)
    }

    private fun scheduleWorkManager(context: Context, config: SmartGeofenceConfig) {
        val repeatMillis = TimeUnit.MINUTES.toMillis(config.reconcileIntervalMinutes)
            .coerceAtLeast(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
        val flexMillis = config.reconcileFlexIntervalMillis.coerceAtLeast(0L)
        val builder = if (flexMillis > 0L) {
            PeriodicWorkRequestBuilder<ReconcileWorker>(
                repeatMillis,
                TimeUnit.MILLISECONDS,
                flexMillis
                    .coerceAtLeast(PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS)
                    .coerceAtMost(repeatMillis),
                TimeUnit.MILLISECONDS,
            )
        } else {
            PeriodicWorkRequestBuilder<ReconcileWorker>(
                repeatMillis,
                TimeUnit.MILLISECONDS,
            )
        }
        val request = builder
            .setInitialDelay(
                config.reconcileInitialDelayMillis.coerceAtLeast(0L),
                TimeUnit.MILLISECONDS,
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(config.reconcileRequiresBatteryNotLow)
                    .build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            Constants.RECONCILE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        SmartGeofenceLogger.d(context, TAG, "WorkManager reconcile scheduled.")
    }

    private fun scheduleAlarm(context: Context, delayMillis: Long): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return try {
            val triggerAt = System.currentTimeMillis() + delayMillis.coerceAtLeast(0L)
            val pending = pendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT)
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Reconcile alarm scheduled triggerAt=$triggerAt exact=$exactAllowed."
            )
            true
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Failed to schedule AlarmManager reconcile: ${e.message}",
                e
            )
            false
        }
    }

    private fun cancelWorkManager(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(Constants.RECONCILE_WORK_NAME)
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        try {
            existingPendingIntent(context)?.let { alarmManager.cancel(it) }
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(
                context,
                TAG,
                "Failed to cancel AlarmManager reconcile: ${e.message}",
                e
            )
        }
    }

    private fun existingPendingIntent(context: Context): PendingIntent? =
        pendingIntent(context, PendingIntent.FLAG_NO_CREATE)

    private fun pendingIntent(context: Context, baseFlags: Int): PendingIntent {
        var flags = baseFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_RECONCILE_ALARM,
            Intent(context, ReconcileAlarmReceiver::class.java),
            flags
        )
    }
}

package com.yarithdev.smart_geofence.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.FenceStore
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceController
import com.yarithdev.smart_geofence.SmartGeofenceLogger

/** AlarmManager-driven reconcile watchdog. */
class ReconcileAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                val config = SmartGeofenceConfigStore.load(appContext)
                if (config.reconcileScheduler != Constants.RECONCILE_SCHEDULER_EXACT_ALARM) {
                    SmartGeofenceLogger.d(
                        appContext,
                        TAG,
                        "Alarm ignored; reconcile scheduler is ${config.reconcileScheduler}."
                    )
                    return@Thread
                }

                if (config.reconcileRequiresBatteryNotLow && isBatteryLow(appContext)) {
                    SmartGeofenceLogger.d(appContext, TAG, "Alarm reconcile skipped: battery is low.")
                } else {
                    SmartGeofenceController.refresh(appContext, scheduleReconcile = false)
                    SmartGeofenceLogger.d(appContext, TAG, "Alarm reconcile: re-armed smart layers.")
                }
            } catch (e: Throwable) {
                SmartGeofenceLogger.w(appContext, TAG, "Alarm reconcile failed: ${e.message}", e)
            } finally {
                val latestConfig = SmartGeofenceConfigStore.load(appContext)
                if (latestConfig.reconcileScheduler == Constants.RECONCILE_SCHEDULER_EXACT_ALARM &&
                    FenceStore.getAll(appContext).isNotEmpty()
                ) {
                    if (!ReconcileScheduler.scheduleNextAlarm(appContext, latestConfig)) {
                        ReconcileScheduler.schedule(appContext, latestConfig)
                    }
                }
                pending.finish()
            }
        }.start()
    }

    private fun isBatteryLow(context: Context): Boolean {
        val battery = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return false
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        if (isCharging) return false

        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return false
        return (level * 100 / scale) <= BATTERY_LOW_PERCENT
    }

    companion object {
        private const val TAG = "ReconcileAlarmReceiver"
        private const val BATTERY_LOW_PERCENT = 15
    }
}

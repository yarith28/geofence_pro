package com.yarithdev.smart_geofence.fencepulse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yarithdev.smart_geofence.SmartGeofenceLogger

/** Alarm tick for the light FencePulse pulse. */
class FencePulseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        SmartGeofenceLogger.d(appContext, TAG, "FencePulse alarm fired.")
        FencePulseController.onTick(appContext)
    }

    companion object {
        private const val TAG = "FencePulseAlarmReceiver"
    }
}

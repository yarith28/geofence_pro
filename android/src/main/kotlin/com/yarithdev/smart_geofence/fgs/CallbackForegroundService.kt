package com.yarithdev.smart_geofence.fgs

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.proximity.LocationConfirmService

class CallbackForegroundService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        try {
            val config = SmartGeofenceConfigStore.load(applicationContext)
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                SmartForegroundNotification.notificationId(config),
                SmartForegroundNotification.build(this, config),
                type
            )
            isRunning = true
            SmartGeofenceLogger.d(
                applicationContext,
                TAG,
                "Callback foreground service started."
            )
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(
                applicationContext,
                TAG,
                "Callback startForeground failed; stopping service: " +
                    "${e.javaClass.simpleName}: ${e.message}",
                e
            )
            releaseWakeLock()
            startRequested = false
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHUTDOWN) {
            releaseWakeLock()
            stopForegroundCompat(removeNotification = !LocationConfirmService.isRunning)
            stopSelf()
            SmartGeofenceLogger.d(
                applicationContext,
                TAG,
                "Callback foreground service stopped."
            )
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (isRunning) {
            stopForegroundCompat(removeNotification = !LocationConfirmService.isRunning)
        }
        isRunning = false
        startRequested = false
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun stopForegroundCompat(removeNotification: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(
                if (removeNotification) {
                    STOP_FOREGROUND_REMOVE
                } else {
                    STOP_FOREGROUND_DETACH
                }
            )
        } else {
            @Suppress("DEPRECATION")
            stopForeground(removeNotification)
        }
    }

    companion object {
        private const val TAG = "CallbackForegroundService"
        private const val ACTION_SHUTDOWN =
            "com.yarithdev.smart_geofence.action.CALLBACK_FOREGROUND_SHUTDOWN"
        private const val WAKE_LOCK_TAG = "smart_geofence:callback_foreground"
        private const val WAKE_LOCK_TIMEOUT_MS = 5 * 60 * 1000L

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        private var startRequested: Boolean = false

        fun promote(context: Context) {
            startRequested = true
            try {
                startForegroundServiceCompat(
                    context,
                    Intent(context, CallbackForegroundService::class.java)
                )
            } catch (e: Exception) {
                startRequested = false
                throw e
            }
        }

        fun demote(context: Context) {
            if (!isRunning && !startRequested) {
                SmartGeofenceLogger.d(
                    context,
                    TAG,
                    "Callback foreground service stop requested while not running or starting."
                )
                return
            }
            startForegroundServiceCompat(
                context,
                Intent(context, CallbackForegroundService::class.java).apply {
                    action = ACTION_SHUTDOWN
                }
            )
        }

        private fun startForegroundServiceCompat(context: Context, intent: Intent) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                SmartGeofenceLogger.w(
                    context,
                    TAG,
                    "Failed to start callback foreground service: " +
                        "${e.javaClass.simpleName}: ${e.message}",
                    e
                )
                throw e
            }
        }
    }
}

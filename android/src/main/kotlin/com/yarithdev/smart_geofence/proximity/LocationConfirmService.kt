package com.yarithdev.smart_geofence.proximity

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import com.yarithdev.smart_geofence.FenceStore
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.fgs.CallbackForegroundService
import com.yarithdev.smart_geofence.fgs.SmartForegroundNotification

class LocationConfirmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var draining = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        isForegroundReady = false
        SmartGeofenceLogger.d(applicationContext, TAG, "Location confirm service created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val attempt = intent?.getIntExtra(LocationConfirmLaunchGate.EXTRA_START_ATTEMPT, 0) ?: 0
        val launchToken = intent
            ?.getLongExtra(LocationConfirmLaunchGate.EXTRA_LAUNCH_TOKEN, 0L)
            ?.takeIf { it > 0L }
            ?: LocationConfirmLaunchGate.ensureLaunchToken(applicationContext, "service start")
        try {
            val config = SmartGeofenceConfigStore.load(applicationContext)
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
            SmartGeofenceLogger.d(
                applicationContext,
                TAG,
                "Promoting location confirm service to foreground token=$launchToken " +
                    "attempt=$attempt type=$type."
            )
            ServiceCompat.startForeground(
                this,
                SmartForegroundNotification.notificationId(config),
                SmartForegroundNotification.build(this, config),
                type
            )
            isForegroundReady = true
            LocationConfirmLaunchGate.onForegroundReady(applicationContext, attempt, launchToken)
            SmartGeofenceLogger.d(
                applicationContext,
                TAG,
                "Location confirm foreground service started token=$launchToken attempt=$attempt " +
                    "pending=${LocationConfirmQueue.count(applicationContext)}."
            )
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(
                applicationContext,
                TAG,
                "Location confirm startForeground failed; stopping service: " +
                    "${e.javaClass.simpleName}: ${e.message}",
                e
            )
            isForegroundReady = false
            LocationConfirmLaunchGate.onForegroundStartFailed(
                applicationContext,
                attempt,
                launchToken,
                "startForeground ${e.javaClass.simpleName}"
            )
            LocationConfirmLaunchGate.scheduleRetry(
                applicationContext,
                attempt,
                "startForeground ${e.javaClass.simpleName}",
                launchToken
            )
            stopSelf()
            return START_NOT_STICKY
        }

        handler.post { drainQueue() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopForegroundCompat(removeNotification = !CallbackForegroundService.isRunning)
        isForegroundReady = false
        isRunning = false
        LocationConfirmLaunchGate.onServiceStopped(applicationContext)
        SmartGeofenceLogger.d(applicationContext, TAG, "Location confirm service destroyed.")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun drainQueue() {
        if (draining) {
            SmartGeofenceLogger.d(applicationContext, TAG, "Confirm queue drain already running.")
            return
        }
        val request = LocationConfirmQueue.peek(applicationContext)
        if (request == null) {
            SmartGeofenceLogger.d(applicationContext, TAG, "Confirm queue empty; stopping service.")
            stopSelf()
            return
        }
        if (request.isNearest) {
            draining = true
            val ageMs = (System.currentTimeMillis() - request.createdAtMillis).coerceAtLeast(0L)
            SmartGeofenceLogger.d(
                applicationContext,
                TAG,
                "Running queued nearest confirm id=${request.id} source=${request.source} " +
                    "age=${ageMs}ms."
            )
            GpsConfirm.confirmNearest(applicationContext, request.source) {
                LocationConfirmQueue.remove(applicationContext, request.id)
                draining = false
                handler.post { drainQueue() }
            }
            return
        }
        val fenceId = request.fenceId
        if (fenceId == null) {
            SmartGeofenceLogger.w(
                applicationContext,
                TAG,
                "Dropping confirm request id=${request.id}; missing fence id."
            )
            LocationConfirmQueue.remove(applicationContext, request.id)
            handler.post { drainQueue() }
            return
        }
        val fence = FenceStore.get(applicationContext, fenceId)
        if (fence == null) {
            SmartGeofenceLogger.w(
                applicationContext,
                TAG,
                "Dropping confirm request id=${request.id}; fence=$fenceId no longer exists."
            )
            LocationConfirmQueue.remove(applicationContext, request.id)
            handler.post { drainQueue() }
            return
        }

        draining = true
        val ageMs = (System.currentTimeMillis() - request.createdAtMillis).coerceAtLeast(0L)
        SmartGeofenceLogger.d(
            applicationContext,
            TAG,
            "Running queued confirm id=${request.id} fence=$fenceId " +
                "source=${request.source} age=${ageMs}ms."
        )
        GpsConfirm.confirm(applicationContext, fence, request.source) {
            LocationConfirmQueue.remove(applicationContext, request.id)
            draining = false
            handler.post { drainQueue() }
        }
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
        private const val TAG = "LocationConfirmService"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var isForegroundReady: Boolean = false
            private set
    }
}

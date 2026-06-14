package com.yarithdev.smart_geofence.fencepulse

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.FenceStore
import com.yarithdev.smart_geofence.SmartGeofenceConfig
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.motion.MotionGate
import com.yarithdev.smart_geofence.proximity.LocationConfirmLaunchGate
import java.util.concurrent.TimeUnit

/**
 * Opt-in light near-boundary pulse. FencePulse itself never starts a foreground
 * service or fetches GPS. It schedules alarm ticks; a tick may enqueue a
 * foreground confirm only when motion suggests a fresh GPS fix is useful.
 */
object FencePulseController {
    private const val TAG = "FencePulseController"

    fun canRun(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!SmartGeofenceConfigStore.load(appContext).fencePulseEnabled) return false
        if (!hasReceiver(appContext, FencePulseAlarmReceiver::class.java)) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse skipped: FencePulseAlarmReceiver not declared.")
            return false
        }
        return true
    }

    fun maybeStart(context: Context): Boolean {
        val appContext = context.applicationContext
        val config = SmartGeofenceConfigStore.load(appContext)
        if (!config.fencePulseEnabled) return false
        if (FenceStore.getAll(appContext).isEmpty()) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse skipped: no fences registered.")
            stop(appContext)
            return false
        }
        if (!canRun(appContext)) return false

        val now = System.currentTimeMillis()
        FencePulseStateStore.ensureStarted(appContext, now)
        if (FencePulseScheduler.pendingIntentExists(appContext)) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse already scheduled.")
            return true
        }

        val delayMs = config.fencePulseExactAlarmStartDelayMillis.coerceAtLeast(0L)
        val scheduled = FencePulseScheduler.schedule(appContext, delayMs)
        if (scheduled) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse started with first tick in ${delayMs}ms.")
        } else {
            SmartGeofenceLogger.w(appContext, TAG, "FencePulse first tick was not scheduled.")
        }
        return scheduled
    }

    fun onTick(context: Context) {
        val appContext = context.applicationContext
        val config = SmartGeofenceConfigStore.load(appContext)
        if (!config.fencePulseEnabled) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse tick ignored; FencePulse disabled.")
            stop(appContext)
            return
        }
        val fenceCount = FenceStore.getAll(appContext).size
        if (fenceCount == 0) {
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse tick stopping; no fences remain.")
            stop(appContext)
            return
        }

        val now = System.currentTimeMillis()
        val state = FencePulseStateStore.ensureStarted(appContext, now)
        val durationMs = TimeUnit.MINUTES.toMillis(config.fencePulseDurationMinutes.coerceAtLeast(0L))
        val elapsedMs = now - state.startedAtMillis
        if (durationMs > 0L && elapsedMs >= durationMs) {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "FencePulse window elapsed; stopping elapsed=${elapsedMs}ms duration=${durationMs}ms."
            )
            stop(appContext)
            return
        }

        val stationary = MotionGate.isLikelyStationary(appContext)
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "FencePulse tick elapsed=${elapsedMs}ms duration=${durationMs}ms " +
                "fences=$fenceCount stationary=$stationary idleTicks=${state.idleTicks}."
        )
        val nextState = if (stationary) {
            val idleTicks = state.idleTicks + 1
            if (idleTicks >= config.fencePulseMaxIdleTicks.coerceAtLeast(1)) {
                SmartGeofenceLogger.d(appContext, TAG, "FencePulse idle; stopping idleTicks=$idleTicks.")
                stop(appContext)
                return
            }
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "FencePulse skipped foreground confirm; stationary idleTicks=$idleTicks."
            )
            state.copy(idleTicks = idleTicks)
        } else {
            val enqueued = LocationConfirmLaunchGate.enqueueNearest(
                appContext,
                Constants.EVENT_SOURCE_SMART_GEOFENCE_FENCE_PULSE_CONFIRM
            )
            if (!enqueued) {
                SmartGeofenceLogger.w(appContext, TAG, "FencePulse nearest confirm was not launched.")
            }
            state.copy(idleTicks = 0)
        }
        FencePulseStateStore.save(appContext, nextState)
        if (!FencePulseScheduler.schedule(appContext, fencePulseIntervalMs(config))) {
            SmartGeofenceLogger.w(appContext, TAG, "FencePulse next tick was not scheduled.")
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        FencePulseScheduler.cancel(appContext)
        FencePulseStateStore.clear(appContext)
        SmartGeofenceLogger.d(appContext, TAG, "FencePulse stopped.")
    }

    private fun fencePulseIntervalMs(config: SmartGeofenceConfig): Long =
        TimeUnit.SECONDS.toMillis(config.fencePulseIntervalSeconds.coerceAtLeast(0L))
            .coerceAtLeast(config.fencePulseMinIntervalMillis.coerceAtLeast(1L))

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
}

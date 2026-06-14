package com.yarithdev.smart_geofence.proximity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.FenceStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceFence
import com.yarithdev.smart_geofence.SmartGeofenceConfig
import com.yarithdev.smart_geofence.LocationPriorityMapper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Precise escalation: spends a single high-accuracy fix to decide whether the
 * device is genuinely inside/outside a nearby fence, then queues the confirmed
 * transition through smart_geofence's postprocessor. Active current-position
 * confirms are run by foreground services after those services have promoted;
 * receiver code may still classify an
 * already-delivered passive/proximity fix without spending a fresh GPS request.
 *
 * NOTE: getCurrentLocation usually resolves within a few seconds. A sustained
 * near-boundary watch (FencePulse) is a separate layer.
 */
object GpsConfirm {
    private const val TAG = "GpsConfirm"

    // Reference fix for the teleport guard.
    private const val KEY_LAST_FIX_LAT = "gpsconfirm_last_lat"
    private const val KEY_LAST_FIX_LNG = "gpsconfirm_last_lng"
    private const val KEY_LAST_FIX_TIME = "gpsconfirm_last_time"

    /** Confirm against a specific (coarsely-identified) nearby fence. */
    fun confirm(
        context: Context,
        fence: SmartGeofenceFence,
        source: String,
        onComplete: () -> Unit
    ) {
        val appContext = context.applicationContext
        val config = SmartGeofenceConfigStore.load(appContext)
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Confirm requested fence=${fence.id} source=$source " +
                "priority=${config.gpsConfirmPriority} " +
                "timeout=${config.gpsConfirmTimeoutMillis}ms."
        )
        fetchHighAccuracy(appContext, source, config, onComplete) { location ->
            handleNearby(appContext, location, fence, source)
        }
    }

    /** Confirm against every registered fence near the fresh fix. */
    fun confirmNearest(context: Context, source: String, onComplete: () -> Unit) {
        val appContext = context.applicationContext
        val config = SmartGeofenceConfigStore.load(appContext)
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Confirm nearest requested source=$source " +
                "priority=${config.gpsConfirmPriority} " +
                "timeout=${config.gpsConfirmTimeoutMillis}ms."
        )
        fetchHighAccuracy(appContext, source, config, onComplete) { location ->
            handleNearby(appContext, location, null, source)
        }
    }

    /**
     * Classify an already-available fix without spending another active
     * location request. Returns true when at least one nearby candidate had a
     * confident inside/outside decision, even if that fence did not subscribe to
     * the corresponding event.
     */
    fun evaluateKnownLocation(
        context: Context,
        location: Location,
        seedFence: SmartGeofenceFence?,
        source: String,
    ): Boolean {
        val appContext = context.applicationContext
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Evaluating known fix source=$source seedFence=${seedFence?.id} " +
                "${describeFix(location)}."
        )
        return handleNearby(appContext, location, seedFence, source)
    }

    private fun handleNearby(
        context: Context,
        location: Location,
        seedFence: SmartGeofenceFence?,
        source: String,
    ): Boolean {
        val fences = FenceStore.getAll(context)
        if (fences.isEmpty()) {
            SmartGeofenceLogger.d(context, TAG, "No registered fences to confirm (source=$source).")
            return false
        }

        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0
        val config = SmartGeofenceConfigStore.load(context)
        val proximityRadius = config.proximityRadiusMeters
        val candidates = linkedMapOf<String, SmartGeofenceFence>()

        // Always re-check the originally selected fence if it still exists;
        // a precise fix may show a confident EXIT even when it is no longer
        // near the boundary.
        if (seedFence != null) {
            fences.firstOrNull { it.id == seedFence.id }?.let { candidates[it.id] = it }
        }
        for (fence in fences) {
            if (edgeDistance(location, fence) - accuracy <= proximityRadius) {
                candidates[fence.id] = fence
            }
        }
        if (candidates.isEmpty()) {
            SmartGeofenceLogger.d(context, TAG, "No nearby fences to confirm (source=$source).")
            return false
        }
        SmartGeofenceLogger.d(
            context,
            TAG,
            "Confirm candidates source=$source seedFence=${seedFence?.id} " +
                "count=${candidates.size} ids=${candidates.keys.joinToString(",")}."
        )
        var decided = false
        candidates.values.forEach { fence ->
            if (handle(context, fence, location, source, config)) {
                decided = true
            }
        }
        return decided
    }

    private fun edgeDistance(location: Location, fence: SmartGeofenceFence): Double {
        val center = Location("smart_geofence").apply {
            latitude = fence.latitude
            longitude = fence.longitude
        }
        return location.distanceTo(center).toDouble() - fence.radiusMeters
    }

    @SuppressLint("MissingPermission")
    private fun fetchHighAccuracy(
        context: Context,
        source: String,
        config: SmartGeofenceConfig,
        onComplete: () -> Unit,
        onFix: (Location) -> Unit,
    ) {
        if (!hasLocationPermission(context)) {
            SmartGeofenceLogger.w(context, TAG, "Skipping confirm: location permission missing (source=$source).")
            onComplete()
            return
        }
        val timeoutMillis = config.gpsConfirmTimeoutMillis.coerceAtLeast(1L)
        val priority = LocationPriorityMapper.toAndroidPriority(config.gpsConfirmPriority)
        SmartGeofenceLogger.d(
            context,
            TAG,
            "Requesting current location source=$source " +
                "priority=${config.gpsConfirmPriority} timeout=${timeoutMillis}ms."
        )
        // Single-fire guard so success / failure / timeout each complete exactly
        // once, and a hung request can't leak the caller's broadcast window.
        val done = AtomicBoolean(false)
        val cts = CancellationTokenSource()
        val handler = Handler(Looper.getMainLooper())
        val timeout = Runnable {
            if (done.compareAndSet(false, true)) {
                cts.cancel()
                SmartGeofenceLogger.w(context, TAG, "Confirm timed out after ${timeoutMillis}ms (source=$source).")
                onComplete()
            }
        }
        handler.postDelayed(timeout, timeoutMillis)

        fun finish(block: () -> Unit) {
            if (done.compareAndSet(false, true)) {
                handler.removeCallbacks(timeout)
                try {
                    block()
                } finally {
                    onComplete()
                }
            }
        }

        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(
                    priority,
                    cts.token
                )
                .addOnSuccessListener { location ->
                    finish {
                        if (location != null) {
                            SmartGeofenceLogger.d(
                                context,
                                TAG,
                                "Confirm fix received source=$source ${describeFix(location)}."
                            )
                            onFix(location)
                        } else {
                            SmartGeofenceLogger.d(context, TAG, "Confirm returned null location (source=$source).")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    finish { SmartGeofenceLogger.w(context, TAG, "Confirm failed (source=$source): ${e.message}", e) }
                }
        } catch (e: SecurityException) {
            finish { SmartGeofenceLogger.w(context, TAG, "Confirm SecurityException (source=$source): ${e.message}", e) }
        }
    }

    private fun handle(
        context: Context,
        fence: SmartGeofenceFence,
        location: Location,
        source: String,
        config: SmartGeofenceConfig,
    ): Boolean {
        val isMock = isMockLocation(location)

        // Teleport guard: reject a fix that implies an impossible speed from the
        // last confirmed fix (stale wifi DB fixes can report good accuracy yet be
        // kilometers off). Skipped for mock fixes so emulator / GPX route testing
        // (which legitimately "teleports") works — matching how the rest of the
        // stack accommodates mock locations rather than blocking them.
        val last = loadLastFix(context)
        if (config.teleportGuardEnabled && !isMock && last != null) {
            val dtSeconds = (location.time - last.time) / 1000.0
            val distance = location.distanceTo(last).toDouble()
            if (TeleportGuard.isImplausible(
                    distance,
                    dtSeconds,
                    config.teleportMaxSpeedMetersPerSecond
                )
            ) {
                SmartGeofenceLogger.w(context, TAG, "Rejecting teleport fix near ${fence.id} source=$source (${distance.toInt()}m / ${dtSeconds}s).")
                return false
            }
        }

        val center = Location("smart_geofence").apply {
            latitude = fence.latitude
            longitude = fence.longitude
        }
        val distance = location.distanceTo(center).toDouble()
        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0
        val decided = when (BoundaryClassifier.classify(
                distance,
                accuracy,
                fence.radiusMeters,
                config.boundaryMinMarginMeters,
                config.boundaryMarginRatio,
                config.boundaryMaxMarginRatio
            )
        ) {
            BoundaryPosition.INSIDE -> {
                if (fence.triggersEnter) {
                    SmartGeofenceLogger.d(context, TAG, "Confirmed INSIDE ${fence.id} source=$source (mock=$isMock) -> queue enter.")
                    SmartGeofencePostProcessor.enqueue(context, fence, "enter", location, isMock, source)
                } else {
                    SmartGeofenceLogger.d(
                        context,
                        TAG,
                        "Confirmed INSIDE ${fence.id} source=$source " +
                            "but enter trigger is not subscribed; no event queued."
                    )
                }
                true
            }
            BoundaryPosition.OUTSIDE -> {
                if (fence.triggersExit) {
                    SmartGeofenceLogger.d(context, TAG, "Confirmed OUTSIDE ${fence.id} source=$source (mock=$isMock) -> queue exit.")
                    SmartGeofencePostProcessor.enqueue(context, fence, "exit", location, isMock, source)
                } else {
                    SmartGeofenceLogger.d(
                        context,
                        TAG,
                        "Confirmed OUTSIDE ${fence.id} source=$source " +
                            "but exit trigger is not subscribed; no event queued."
                    )
                }
                true
            }
            BoundaryPosition.AMBIGUOUS -> {
                SmartGeofenceLogger.d(context, TAG, "Ambiguous near ${fence.id} source=$source; no decision.")
                false
            }
        }
        // Don't let a mock fix become the teleport-guard reference for a later
        // real fix.
        if (!isMock) saveLastFix(context, location)
        return decided
    }

    @Suppress("DEPRECATION")
    private fun isMockLocation(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock
        else location.isFromMockProvider

    private fun loadLastFix(context: Context): Location? {
        val p = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!p.contains(KEY_LAST_FIX_LAT)) return null
        return Location("smart_geofence_confirm").apply {
            latitude = Double.fromBits(p.getLong(KEY_LAST_FIX_LAT, 0))
            longitude = Double.fromBits(p.getLong(KEY_LAST_FIX_LNG, 0))
            time = p.getLong(KEY_LAST_FIX_TIME, 0)
        }
    }

    private fun saveLastFix(context: Context, location: Location) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_FIX_LAT, location.latitude.toRawBits())
            .putLong(KEY_LAST_FIX_LNG, location.longitude.toRawBits())
            .putLong(KEY_LAST_FIX_TIME, location.time)
            .apply()
    }

    private fun describeFix(location: Location): String {
        val provider = location.provider ?: "unknown"
        val accuracy = if (location.hasAccuracy()) "${location.accuracy.toInt()}m" else "unknown"
        val age = if (location.time > 0L) {
            "${(System.currentTimeMillis() - location.time).coerceAtLeast(0L)}ms"
        } else {
            "unknown"
        }
        return "provider=$provider acc=$accuracy age=$age mock=${isMockLocation(location)}"
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

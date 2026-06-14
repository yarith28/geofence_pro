package com.yarithdev.smart_geofence.proximity

import kotlin.math.max

/**
 * Pure, side-effect-free decision logic for the escalation layer. Kept separate
 * from the Android plumbing so it can be unit-tested on the JVM.
 */

enum class BoundaryPosition { INSIDE, OUTSIDE, AMBIGUOUS }

object BoundaryClassifier {
    const val MIN_MARGIN_METERS = 20.0
    const val MARGIN_RATIO = 0.10
    const val MAX_MARGIN_RATIO = 0.50

    /**
     * Accuracy-aware classification with a dead-band around the edge: a fix is
     * only INSIDE/OUTSIDE if it clears the boundary by its own accuracy plus a
     * margin, otherwise AMBIGUOUS (no confident decision -> no event).
     */
    fun classify(
        distanceMeters: Double,
        accuracyMeters: Double,
        radiusMeters: Double,
        minMarginMeters: Double = MIN_MARGIN_METERS,
        marginRatio: Double = MARGIN_RATIO,
        maxMarginRatio: Double = MAX_MARGIN_RATIO,
    ): BoundaryPosition {
        val margin = max(minMarginMeters.coerceAtLeast(0.0), radiusMeters * marginRatio.coerceAtLeast(0.0))
            .coerceAtMost(radiusMeters * maxMarginRatio.coerceIn(0.0, 1.0))
        return when {
            distanceMeters + accuracyMeters <= radiusMeters - margin -> BoundaryPosition.INSIDE
            distanceMeters - accuracyMeters >= radiusMeters + margin -> BoundaryPosition.OUTSIDE
            else -> BoundaryPosition.AMBIGUOUS
        }
    }
}

object TeleportGuard {
    // ~250 km/h: above any plausible ground speed, so it flags GPS/wifi glitches.
    const val MAX_SPEED_MPS = 70.0

    /**
     * True if moving [distanceMeters] in [dtSeconds] implies an impossible speed
     * (a teleport / bad fix). A non-positive dt is treated as not implausible
     * (insufficient information).
     */
    fun isImplausible(
        distanceMeters: Double,
        dtSeconds: Double,
        maxSpeedMps: Double = MAX_SPEED_MPS,
    ): Boolean {
        if (dtSeconds <= 0.0) return false
        return distanceMeters / dtSeconds > maxSpeedMps
    }
}

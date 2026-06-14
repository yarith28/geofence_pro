package com.yarithdev.smart_geofence.proximity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundaryClassifierTest {
    // radius 150 -> margin = max(20, 15) = 20.

    @Test
    fun wellInside_isInside() {
        // distance 100 + accuracy 10 = 110 <= 150 - 20 = 130.
        assertEquals(BoundaryPosition.INSIDE, BoundaryClassifier.classify(100.0, 10.0, 150.0))
    }

    @Test
    fun wellOutside_isOutside() {
        // distance 200 - accuracy 10 = 190 >= 150 + 20 = 170.
        assertEquals(BoundaryPosition.OUTSIDE, BoundaryClassifier.classify(200.0, 10.0, 150.0))
    }

    @Test
    fun nearEdge_isAmbiguous() {
        // distance 150, accuracy 10: neither inside nor outside band.
        assertEquals(BoundaryPosition.AMBIGUOUS, BoundaryClassifier.classify(150.0, 10.0, 150.0))
    }

    @Test
    fun poorAccuracyWidensDeadBand() {
        // distance 100, accuracy 60: 160 > 130 so not INSIDE -> ambiguous.
        assertEquals(BoundaryPosition.AMBIGUOUS, BoundaryClassifier.classify(100.0, 60.0, 150.0))
    }

    @Test
    fun largeRadiusUsesRatioMargin() {
        // radius 2000 -> margin = max(20, 200) = 200.
        // distance 1700 + acc 50 = 1750 <= 2000 - 200 = 1800 -> inside.
        assertEquals(BoundaryPosition.INSIDE, BoundaryClassifier.classify(1700.0, 50.0, 2000.0))
    }
}

class TeleportGuardTest {
    @Test
    fun plausibleWalk_isOk() {
        // 100 m in 60 s ≈ 1.7 m/s.
        assertFalse(TeleportGuard.isImplausible(100.0, 60.0))
    }

    @Test
    fun teleport_isImplausible() {
        // 5 km in 10 s = 500 m/s.
        assertTrue(TeleportGuard.isImplausible(5000.0, 10.0))
    }

    @Test
    fun zeroOrNegativeDt_isNotImplausible() {
        assertFalse(TeleportGuard.isImplausible(5000.0, 0.0))
        assertFalse(TeleportGuard.isImplausible(5000.0, -5.0))
    }

    @Test
    fun highwaySpeed_isOk() {
        // 30 km in 30 min = 16.7 m/s (~60 km/h).
        assertFalse(TeleportGuard.isImplausible(30000.0, 1800.0))
    }
}

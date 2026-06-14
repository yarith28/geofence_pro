package com.yarithdev.smart_geofence

import com.google.android.gms.location.Priority

/** Maps Dart SmartGeofenceLocationPriority names to Play Services constants. */
object LocationPriorityMapper {
    fun toAndroidPriority(name: String): Int =
        when (name) {
            Constants.DEFAULT_LOCATION_PRIORITY_HIGH_ACCURACY ->
                Priority.PRIORITY_HIGH_ACCURACY
            Constants.DEFAULT_LOCATION_PRIORITY_LOW_POWER ->
                Priority.PRIORITY_LOW_POWER
            Constants.DEFAULT_LOCATION_PRIORITY_PASSIVE ->
                Priority.PRIORITY_PASSIVE
            else -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
}

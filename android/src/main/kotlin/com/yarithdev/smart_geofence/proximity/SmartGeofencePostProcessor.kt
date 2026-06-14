package com.yarithdev.smart_geofence.proximity

import android.content.Context
import android.location.Location
import com.chunkytofustudios.native_geofence.generated.ActiveGeofenceWire
import com.chunkytofustudios.native_geofence.generated.GeofenceCallbackParamsWire
import com.chunkytofustudios.native_geofence.generated.GeofenceEvent
import com.chunkytofustudios.native_geofence.generated.LocationWire
import com.chunkytofustudios.native_geofence.util.ActiveGeofenceWires
import com.chunkytofustudios.native_geofence.util.GeofenceCallbackWork
import com.chunkytofustudios.native_geofence.util.NativeGeofencePersistence
import com.yarithdev.smart_geofence.SmartGeofenceFence
import com.yarithdev.smart_geofence.SmartGeofenceLogger

/**
 * Post-processes transitions confirmed by smart_geofence's pre-processing
 * layers. The OS geofence path still belongs to native_geofence; this path owns
 * smart-confirmed transitions and feeds them directly into the callback worker.
 *
 * It shares native_geofence's delivered-state store so an OS transition and a
 * smart-confirmed transition for the same fence state do not double-fire.
 */
object SmartGeofencePostProcessor {
    private const val TAG = "SmartGeofencePostProcessor"

    fun enqueue(
        context: Context,
        fence: SmartGeofenceFence,
        eventName: String,
        location: Location?,
        isMock: Boolean,
        source: String,
    ): Boolean {
        val appContext = context.applicationContext
        val event = eventFromName(eventName)
        if (event == null) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Ignoring confirmed event with invalid event=$eventName fence=${fence.id} source=$source."
            )
            return false
        }
        if (!fence.subscribesTo(event)) {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Ignoring confirmed $eventName for ${fence.id}; trigger is not subscribed."
            )
            return false
        }

        val claimedAt = System.currentTimeMillis()
        if (!NativeGeofencePersistence.claimDeliveredGeofenceEvent(
                appContext,
                fence.id,
                event,
                claimedAt
            )
        ) {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirmed event de-duped fence=${fence.id} event=$event source=$source."
            )
            return false
        }

        val params = GeofenceCallbackParamsWire(
            geofences = listOf(activeGeofenceWire(appContext, fence)),
            event = event,
            location = location?.toLocationWire(isMock),
            callbackHandle = fence.callbackHandle,
        )
        GeofenceCallbackWork.enqueue(appContext, params, source) { enqueued ->
            if (!enqueued) {
                NativeGeofencePersistence.releaseDeliveredGeofenceEventClaim(
                    appContext,
                    fence.id,
                    event,
                    claimedAt
                )
            }
        }
        SmartGeofenceLogger.i(
            appContext,
            TAG,
            "Queued confirmed event fence=${fence.id} event=$event source=$source."
        )
        return true
    }

    private fun eventFromName(eventName: String): GeofenceEvent? =
        when (eventName.lowercase()) {
            "enter" -> GeofenceEvent.ENTER
            "exit" -> GeofenceEvent.EXIT
            "dwell" -> GeofenceEvent.DWELL
            else -> null
        }

    private fun SmartGeofenceFence.subscribesTo(event: GeofenceEvent): Boolean =
        when (event) {
            GeofenceEvent.ENTER -> triggersEnter
            GeofenceEvent.EXIT -> triggersExit
            GeofenceEvent.DWELL -> triggersDwell
        }

    private fun activeGeofenceWire(
        context: Context,
        fence: SmartGeofenceFence,
    ): ActiveGeofenceWire {
        val nativeFence = NativeGeofencePersistence.getGeofence(context, fence.id)
        if (nativeFence != null) {
            return ActiveGeofenceWires.fromGeofenceWire(nativeFence)
        }
        return ActiveGeofenceWire(
            id = fence.id,
            location = LocationWire(
                latitude = fence.latitude,
                longitude = fence.longitude,
                accuracyMeters = null,
                isMock = false,
            ),
            radiusMeters = fence.radiusMeters,
            triggers = buildList {
                if (fence.triggersEnter) add(GeofenceEvent.ENTER)
                if (fence.triggersExit) add(GeofenceEvent.EXIT)
                if (fence.triggersDwell) add(GeofenceEvent.DWELL)
            },
            androidSettings = null,
        )
    }

    private fun Location.toLocationWire(isMock: Boolean): LocationWire =
        LocationWire(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
            isMock = isMock,
        )
}

package com.yarithdev.smart_geofence

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * smart_geofence's own mirror of the registered fence geometry.
 *
 * Why a separate store: background components (ProximityReceiver, workers) run
 * with no live Dart isolate and need smart_geofence-owned geometry for
 * proximity classification. Since smart_geofence is the single entry point, it
 * records that geometry here whenever the app registers/removes a fence.
 * Post-classification callback queueing can still share native_geofence's
 * delivered-state store so OS events and smart-confirmed events de-dupe
 * together.
 */
data class SmartGeofenceFence(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val triggersEnter: Boolean,
    val triggersExit: Boolean,
    val triggersDwell: Boolean,
    val callbackHandle: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("lat", latitude)
        put("lng", longitude)
        put("radius", radiusMeters)
        put("enter", triggersEnter)
        put("exit", triggersExit)
        put("dwell", triggersDwell)
        put("cb", callbackHandle)
    }

    fun toMap(): Map<String, Any?> {
        val triggers = mutableListOf<String>()
        if (triggersEnter) triggers.add("enter")
        if (triggersExit) triggers.add("exit")
        if (triggersDwell) triggers.add("dwell")
        return linkedMapOf(
            "id" to id,
            "latitude" to latitude,
            "longitude" to longitude,
            "radiusMeters" to radiusMeters,
            "triggers" to triggers,
            "callbackHandle" to callbackHandle,
        )
    }

    companion object {
        fun fromJson(o: JSONObject): SmartGeofenceFence = SmartGeofenceFence(
            id = o.getString("id"),
            latitude = o.getDouble("lat"),
            longitude = o.getDouble("lng"),
            radiusMeters = o.getDouble("radius"),
            triggersEnter = o.optBoolean("enter", false),
            triggersExit = o.optBoolean("exit", false),
            triggersDwell = o.optBoolean("dwell", false),
            callbackHandle = o.optLong("cb", 0L),
        )
    }
}

object FenceStore {
    private const val TAG = "FenceStore"
    private const val KEY_FENCES = "pro_fences"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun upsert(context: Context, fence: SmartGeofenceFence): SmartGeofenceFence? {
        val all = getAll(context).associateBy { it.id }.toMutableMap()
        val previous = all[fence.id]
        all[fence.id] = fence
        persist(context, all.values)
        return previous
    }

    fun remove(context: Context, id: String) {
        val all = getAll(context).filterNot { it.id == id }
        persist(context, all)
    }

    fun removeAll(context: Context) {
        persist(context, emptyList())
    }

    fun getAll(context: Context): List<SmartGeofenceFence> {
        val raw = prefs(context).getString(KEY_FENCES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { SmartGeofenceFence.fromJson(arr.getJSONObject(it)) }
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(context, TAG, "Failed to parse fence store: ${e.message}", e)
            emptyList()
        }
    }

    fun get(context: Context, id: String): SmartGeofenceFence? = getAll(context).firstOrNull { it.id == id }

    private fun persist(context: Context, fences: Collection<SmartGeofenceFence>) {
        val arr = JSONArray()
        fences.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_FENCES, arr.toString()).apply()
    }
}

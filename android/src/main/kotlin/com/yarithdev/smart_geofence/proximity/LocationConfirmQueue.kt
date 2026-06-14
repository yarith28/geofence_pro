package com.yarithdev.smart_geofence.proximity

import android.content.Context
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import org.json.JSONArray
import org.json.JSONObject

data class LocationConfirmRequest(
    val id: Long,
    val fenceId: String?,
    val source: String,
    val createdAtMillis: Long,
) {
    val isNearest: Boolean
        get() = fenceId == null
}

object LocationConfirmQueue {
    private const val TAG = "LocationConfirmQueue"
    private const val KEY_QUEUE = "location_confirm_queue"
    private const val KEY_NEXT_ID = "location_confirm_next_id"
    private const val MAX_PENDING_REQUESTS = 32

    @Synchronized
    fun enqueue(context: Context, fenceId: String, source: String): LocationConfirmRequest {
        val appContext = context.applicationContext
        val queue = read(appContext).toMutableList()
        queue.firstOrNull { it.fenceId == fenceId && it.source == source }?.let {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Confirm request already queued id=${it.id} fence=$fenceId source=$source."
            )
            return it
        }

        val prefs = prefs(appContext)
        val nextId = prefs.getLong(KEY_NEXT_ID, 0L) + 1L
        val request = LocationConfirmRequest(
            id = nextId,
            fenceId = fenceId,
            source = source,
            createdAtMillis = System.currentTimeMillis(),
        )
        queue.add(request)
        val trimmed = queue.takeLast(MAX_PENDING_REQUESTS)
        persist(appContext, trimmed, nextId)
        if (trimmed.size != queue.size) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Dropped ${queue.size - trimmed.size} old confirm request(s); queue limit=$MAX_PENDING_REQUESTS."
            )
        }
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Queued confirm request id=${request.id} fence=$fenceId source=$source pending=${trimmed.size}."
        )
        return request
    }

    @Synchronized
    fun enqueueNearest(context: Context, source: String): LocationConfirmRequest {
        val appContext = context.applicationContext
        val queue = read(appContext).toMutableList()
        queue.firstOrNull { it.isNearest && it.source == source }?.let {
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Nearest confirm request already queued id=${it.id} source=$source."
            )
            return it
        }

        val prefs = prefs(appContext)
        val nextId = prefs.getLong(KEY_NEXT_ID, 0L) + 1L
        val request = LocationConfirmRequest(
            id = nextId,
            fenceId = null,
            source = source,
            createdAtMillis = System.currentTimeMillis(),
        )
        queue.add(request)
        val trimmed = queue.takeLast(MAX_PENDING_REQUESTS)
        persist(appContext, trimmed, nextId)
        if (trimmed.size != queue.size) {
            SmartGeofenceLogger.w(
                appContext,
                TAG,
                "Dropped ${queue.size - trimmed.size} old confirm request(s); queue limit=$MAX_PENDING_REQUESTS."
            )
        }
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Queued nearest confirm request id=${request.id} source=$source pending=${trimmed.size}."
        )
        return request
    }

    @Synchronized
    fun peek(context: Context): LocationConfirmRequest? =
        read(context.applicationContext).firstOrNull()

    @Synchronized
    fun remove(context: Context, id: Long) {
        val appContext = context.applicationContext
        val queue = read(appContext)
        val remaining = queue.filterNot { it.id == id }
        if (remaining.size != queue.size) {
            persist(appContext, remaining)
            SmartGeofenceLogger.d(appContext, TAG, "Removed confirm request id=$id pending=${remaining.size}.")
        }
    }

    @Synchronized
    fun removeFence(context: Context, fenceId: String) {
        val appContext = context.applicationContext
        val queue = read(appContext)
        val remaining = queue.filterNot { it.fenceId == fenceId }
        if (remaining.size != queue.size) {
            persist(appContext, remaining)
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Removed ${queue.size - remaining.size} confirm request(s) for fence=$fenceId pending=${remaining.size}."
            )
        }
    }

    @Synchronized
    fun clear(context: Context) {
        val appContext = context.applicationContext
        persist(appContext, emptyList())
        SmartGeofenceLogger.d(appContext, TAG, "Cleared pending confirm requests.")
    }

    @Synchronized
    fun count(context: Context): Int = read(context.applicationContext).size

    private fun read(context: Context): List<LocationConfirmRequest> {
        val raw = prefs(context).getString(KEY_QUEUE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { index ->
                try {
                    fromJson(arr.getJSONObject(index))
                } catch (e: Throwable) {
                    SmartGeofenceLogger.w(context, TAG, "Skipping malformed confirm request: ${e.message}", e)
                    null
                }
            }
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(context, TAG, "Failed to parse confirm queue: ${e.message}", e)
            emptyList()
        }
    }

    private fun persist(
        context: Context,
        requests: List<LocationConfirmRequest>,
        nextId: Long? = null,
    ) {
        val arr = JSONArray()
        requests.forEach { arr.put(toJson(it)) }
        val editor = prefs(context).edit().putString(KEY_QUEUE, arr.toString())
        if (nextId != null) editor.putLong(KEY_NEXT_ID, nextId)
        editor.apply()
    }

    private fun toJson(request: LocationConfirmRequest): JSONObject = JSONObject().apply {
        put("id", request.id)
        put("source", request.source)
        put("createdAt", request.createdAtMillis)
        if (request.fenceId == null) {
            put("nearest", true)
        } else {
            put("fenceId", request.fenceId)
        }
    }

    private fun fromJson(o: JSONObject): LocationConfirmRequest? {
        val source = o.optString("source").takeIf { it.isNotBlank() } ?: return null
        val fenceId = o.optString("fenceId").takeIf { it.isNotBlank() }
        if (fenceId == null && !o.optBoolean("nearest", false)) return null
        return LocationConfirmRequest(
            id = o.getLong("id"),
            fenceId = fenceId,
            source = source,
            createdAtMillis = o.optLong("createdAt", 0L),
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
}

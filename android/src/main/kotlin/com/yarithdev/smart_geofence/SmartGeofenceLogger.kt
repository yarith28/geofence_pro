package com.yarithdev.smart_geofence

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single logging bridge for Android smart layers.
 *
 * Messages always go to logcat. When file logging is enabled, the same messages
 * are appended to a bounded app-private file so the host app can fetch/export
 * them later, including after background receivers/workers ran without Dart.
 */
object SmartGeofenceLogger {
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    @Volatile
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun configure(context: Context, enabled: Boolean, maxBytes: Int) {
        initialize(context)
        context.applicationContext
            .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.CONFIG_LOG_FILE_ENABLED, enabled)
            .putInt(Constants.CONFIG_MAX_LOG_FILE_BYTES, normalizeMaxBytes(maxBytes))
            .apply()
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        append(null, "debug", tag, message)
    }

    fun d(context: Context, tag: String, message: String) {
        Log.d(tag, message)
        append(context, "debug", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append(null, "info", tag, message)
    }

    fun i(context: Context, tag: String, message: String) {
        Log.i(tag, message)
        append(context, "info", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
        append(null, "warning", tag, message, throwable)
    }

    fun w(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
        append(context, "warning", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
        append(null, "error", tag, message, throwable)
    }

    fun e(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
        append(context, "error", tag, message, throwable)
    }

    fun fgs(
        context: Context,
        service: String,
        stage: String,
        token: Long,
        attempt: Int? = null,
        detail: String? = null,
    ) {
        i(context, "SmartGeofenceFgs", fgsMessage(service, stage, token, attempt, detail))
    }

    fun fgsWarning(
        context: Context,
        service: String,
        stage: String,
        token: Long,
        attempt: Int? = null,
        detail: String? = null,
        throwable: Throwable? = null,
    ) {
        w(context, "SmartGeofenceFgs", fgsMessage(service, stage, token, attempt, detail), throwable)
    }

    fun read(context: Context): String {
        initialize(context)
        val file = logFile(context.applicationContext)
        return if (file.exists()) file.readText(Charsets.UTF_8) else ""
    }

    fun clear(context: Context) {
        initialize(context)
        val file = logFile(context.applicationContext)
        if (file.exists()) file.writeText("", Charsets.UTF_8)
    }

    private fun append(
        context: Context?,
        level: String,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val ctx = context?.applicationContext ?: appContext ?: return
        if (!isEnabled(ctx)) return
        synchronized(this) {
            val file = logFile(ctx)
            file.parentFile?.mkdirs()
            file.appendText(formatLine(level, tag, message, throwable), Charsets.UTF_8)
            trimToMaxBytes(file, maxBytes(ctx))
        }
    }

    private fun formatLine(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable?,
    ): String {
        val timestamp = synchronized(timestampFormat) {
            timestampFormat.format(Date(System.currentTimeMillis()))
        }
        return buildString {
            append(timestamp)
            append(" [")
            append(level)
            append("] ")
            append(tag)
            append(": ")
            append(message)
            append('\n')
            if (throwable != null) {
                append(Log.getStackTraceString(throwable))
                if (isEmpty() || this[length - 1] != '\n') append('\n')
            }
        }
    }

    private fun fgsMessage(
        service: String,
        stage: String,
        token: Long,
        attempt: Int?,
        detail: String?,
    ): String {
        val attemptPart = attempt?.let { " attempt=$it" } ?: ""
        val detailPart = detail?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "FGS[$service] $stage token=$token$attemptPart$detailPart"
    }

    private fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(Constants.CONFIG_LOG_FILE_ENABLED, false)

    private fun maxBytes(context: Context): Int =
        normalizeMaxBytes(
            prefs(context).getInt(
                Constants.CONFIG_MAX_LOG_FILE_BYTES,
                Constants.DEFAULT_LOG_FILE_MAX_BYTES
            )
        )

    private fun normalizeMaxBytes(maxBytes: Int): Int =
        maxBytes.coerceIn(Constants.MIN_LOG_FILE_MAX_BYTES, Constants.MAX_LOG_FILE_MAX_BYTES)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private fun logFile(context: Context): File =
        File(context.applicationContext.noBackupFilesDir, Constants.LOG_FILE_NAME)

    private fun trimToMaxBytes(file: File, maxBytes: Int) {
        if (!file.exists() || file.length() <= maxBytes) return
        val bytes = file.readBytes()
        val keep = maxBytes.coerceAtMost(bytes.size)
        val start = bytes.size - keep
        var trimmed = bytes.copyOfRange(start, bytes.size)
        val newline = trimmed.indexOf('\n'.code.toByte())
        if (newline >= 0 && newline < trimmed.lastIndex) {
            trimmed = trimmed.copyOfRange(newline + 1, trimmed.size)
        }
        file.writeBytes(trimmed)
    }
}

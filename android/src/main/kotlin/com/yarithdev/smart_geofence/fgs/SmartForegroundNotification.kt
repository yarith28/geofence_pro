package com.yarithdev.smart_geofence.fgs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceConfig

object SmartForegroundNotification {
    fun build(context: Context, config: SmartGeofenceConfig): Notification {
        val channelId = config.fencePulseNotificationChannelId.ifBlank {
            Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_ID
        }
        val channelName = config.fencePulseNotificationChannelName.ifBlank {
            Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(
                config.fencePulseNotificationTitle.ifBlank {
                    Constants.DEFAULT_CONFIRM_NOTIFICATION_TITLE
                }
            )
            .setSmallIcon(
                resolveSmallIcon(context, config.fencePulseNotificationSmallIconResourceName)
            )
            .setOngoing(true)
            .build()
    }

    fun notificationId(config: SmartGeofenceConfig): Int =
        config.fencePulseNotificationId.takeIf { it > 0 }
            ?: Constants.DEFAULT_CONFIRM_NOTIFICATION_ID

    private fun resolveSmallIcon(context: Context, resourceName: String?): Int {
        val name = resourceName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return android.R.drawable.ic_menu_mylocation
        val resources = context.resources
        val packageName = context.packageName
        val id = when {
            ":" in name -> resources.getIdentifier(name, null, null)
            "/" in name -> {
                val parts = name.split("/", limit = 2)
                resources.getIdentifier(parts[1], parts[0], packageName)
            }
            else -> resources.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }
                ?: resources.getIdentifier(name, "mipmap", packageName)
        }
        return id.takeIf { it != 0 } ?: android.R.drawable.ic_menu_mylocation
    }
}

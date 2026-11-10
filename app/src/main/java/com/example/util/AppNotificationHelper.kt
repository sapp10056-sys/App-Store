package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ApplicationItem

/**
 * Helper to display Android status-bar notifications when developers upload new APKs to NovaStore.
 */
object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"
    const val CHANNEL_ID = "novastore_new_apk_uploads"
    const val CHANNEL_NAME = "New APK Uploads"
    const val CHANNEL_DESCRIPTION = "Alerts when developers upload new applications to NovaStore"

    fun initChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNewAppNotification(context: Context, app: ApplicationItem) {
        try {
            initChannel(context)

            // Check permission on Android 13+ (TIRAMISU)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Notification permission not granted, skipping status bar notification")
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = Uri.parse("novastore://app/${app.appId}")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                app.appId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "🚀 New APK Uploaded: ${app.appName}"
            val content = "${app.developerName} published ${app.appName} (v${app.versionName}). Tap to view and install on NovaStore!"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(app.appId.hashCode(), builder.build())
            Log.d(TAG, "Displayed push notification for new app: ${app.appName}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to display new app notification: ${e.message}")
        }
    }
}

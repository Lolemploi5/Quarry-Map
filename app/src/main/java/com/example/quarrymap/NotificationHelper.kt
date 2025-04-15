package com.example.quarrymap

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Classe utilitaire pour gérer les notifications avec barre de progression
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "quarry_map_download_channel"
        private const val DOWNLOAD_NOTIFICATION_ID = 1001
        private const val TAG = "NotificationHelper"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Téléchargements"
            val descriptionText = "Notifications pour les téléchargements d'images"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showProgressNotification(
        title: String,
        content: String,
        progress: Int,
        max: Int,
        indeterminate: Boolean = false
    ) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(title)
                setContentText(content)
                setSmallIcon(R.drawable.ic_download)
                setOngoing(true)
                priority = NotificationCompat.PRIORITY_LOW
                setProgress(max, progress, indeterminate)
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
                }
            } else {
                Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showCompletedNotification(title: String, content: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setContentTitle(title)
                setContentText(content)
                setSmallIcon(R.drawable.ic_download_complete)
                setProgress(0, 0, false)
                setOngoing(false)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
                }
            } else {
                Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotification() {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(DOWNLOAD_NOTIFICATION_ID)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

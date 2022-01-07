package com.durvank883.tinier.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.durvank883.tinier.R
import com.durvank883.tinier.service.ImageCompressorService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ImageCompressorNotification @Inject constructor(
    @ApplicationContext val context: Context,
) {

    companion object {
        private const val CHANNEL_ID = "TINIER_COMPRESSOR"
        private const val PROGRESS_NOTIFICATION_ID = 131313
    }

    private lateinit var builder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    init {
        createNotificationChannel()
    }

    fun init(
        contentTitle: String = "Tinier",
        contentText: String = "Starting",
        smallIcon: Int = R.drawable.tinier_logo,
        color: Int = Color.Black.toArgb(),
        priority: Int = NotificationCompat.PRIORITY_LOW
    ): ImageCompressorNotification {

        val notificationIntent = Intent(context, ImageCompressorService::class.java).apply {
            action = "STOP"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            notificationIntent,
            flags
        )

        builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentTitle(contentTitle)
            setContentText(contentText)
            setSmallIcon(smallIcon)
            setColor(color)
            setPriority(priority)
            setCategory(NotificationCompat.CATEGORY_PROGRESS)
            setAutoCancel(false)
            setOngoing(true)
            setSilent(true)
            addAction(R.drawable.ic_stop_24, "Stop", pendingIntent)
        }
        return this
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, "Tinier Channel", importance).apply {
                description = "Compression progress notification"
            }
            // Register the channel with the system
            val notificationManager = getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun startNotifying() {
        notificationManager = NotificationManagerCompat.from(context).apply {
            builder.setProgress(100, 0, false)
            notify(PROGRESS_NOTIFICATION_ID, builder.build())
        }
    }

    fun updateContentText(contextText: String) {
        notificationManager.apply {
            builder.setContentText(contextText)
            notify(PROGRESS_NOTIFICATION_ID, builder.build())
        }
    }

    fun updateProgress(maxProgress: Int, currentProgress: Int) {
        notificationManager.apply {
            builder
                .setContentText("Compressing")
                .setProgress(maxProgress, currentProgress, false)
            notify(PROGRESS_NOTIFICATION_ID, builder.build())
        }
    }

    fun removeActions() {
        notificationManager.apply {
            builder
                .setProgress(0, 0, false)
                .clearActions()
            notify(PROGRESS_NOTIFICATION_ID, builder.build())
        }
    }

    fun completeProgress(contentText: String = "Compression complete") {
        notificationManager.apply {
            builder
                .setContentText(contentText)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .clearActions()
            notify(PROGRESS_NOTIFICATION_ID, builder.build())
        }
    }

    fun dismissNotifications() {
        notificationManager.cancelAll()
    }
}
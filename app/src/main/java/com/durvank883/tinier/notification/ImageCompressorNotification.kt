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
import javax.inject.Singleton

@Singleton
class ImageCompressorNotification @Inject constructor(
    @ApplicationContext val context: Context,
) {

    companion object {
        private const val CHANNEL_ID = "TINIER_COMPRESSOR"
        const val PROGRESS_NOTIFICATION_ID = 131313

    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    private val notificationManager = NotificationManagerCompat.from(context)

    private var showNotifications = false

    init {
        createNotificationChannel()
        init()
    }

    private fun init(
        contentTitle: String = "Tinier",
        contentText: String = "Starting",
        smallIcon: Int = R.drawable.tinier_logo,
        color: Int = Color.Black.toArgb(),
        priority: Int = NotificationCompat.PRIORITY_LOW
    ) {
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

        builder.apply {
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

    fun showNotifications(show: Boolean) {
        this.showNotifications = show
    }

    fun updateContentText(contextText: String) {
        if (showNotifications) {
            notificationManager.apply {
                builder.setContentText(contextText)
                notify(PROGRESS_NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun updateProgress(maxProgress: Int, currentProgress: Int) {
        if (showNotifications) {
            notificationManager.apply {
                builder
                    .setContentText("Compressing")
                    .setProgress(maxProgress, currentProgress, false)
                notify(PROGRESS_NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun removeActions() {
        if (showNotifications) {
            notificationManager.apply {
                builder
                    .setContentText("Stopping")
                    .setProgress(0, 0, false)
                    .clearActions()
                notify(PROGRESS_NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun completeProgress(contentText: String = "Compression complete") {
        if (showNotifications) {
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
    }

    fun dismissNotifications() {
        if (showNotifications) {
            notificationManager.apply {
                builder
                    .clearActions()
                    .setProgress(0, 0, false)
                cancelAll()
            }
//            showNotifications(show = false)
        }
    }
}
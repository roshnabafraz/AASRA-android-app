package com.roshnab.aasra.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roshnab.aasra.R
import java.util.concurrent.atomic.AtomicInteger

object LocalPushHelper {
    private const val CHANNEL_ID = "aasra_notifications"
    private var appContext: Context? = null
    private val notificationIdGen = AtomicInteger(0)

    fun init(context: Context) {
        appContext = context.applicationContext
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AASRA Alerts"
            val descriptionText = "Emergency alerts and request updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String) {
        val context = appContext ?: return
        
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Fallback icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(notificationIdGen.incrementAndGet(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
            e.printStackTrace()
        }
    }
}

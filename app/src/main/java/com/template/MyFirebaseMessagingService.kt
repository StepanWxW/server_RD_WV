package com.template

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notificationTitle = remoteMessage.notification?.title

        val notificationBody = remoteMessage.notification?.body

         val notification = NotificationCompat.Builder(this, "channel_id")
             .setContentTitle(notificationTitle)
             .setContentText(notificationBody)
             .setSmallIcon(android.R.drawable.ic_dialog_info)
             .build()

         val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(777, notification)
    }
}
package com.example.agoraapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FCM", "Notification Title: ${it.title}")
            Log.d("FCM", "Notification Body: ${it.body}")
            // TODO: Optionally show local notification
        }

        remoteMessage.data.let {
            Log.d("FCM", "Data Payload: $it")
            // TODO: Handle custom data payload
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "New FCM Token: $token")
        // TODO: Send token to your backend if necessary
    }
}

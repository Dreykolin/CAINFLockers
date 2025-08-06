package com.example.cainflockers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Nueva Solicitud"
            val messageBody = remoteMessage.data["body"] ?: "Se ha enviado una nueva solicitud de locker."
            Log.d(TAG, "Preparing to send notification with title: $title and body: $messageBody")
            sendNotification(title, messageBody)
        } else {
            Log.d(TAG, "No data payload found in the message")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Envía este token a tu backend si tienes uno para gestionar notificaciones dirigidas
        sendTokenToBackend(token)
    }
    private fun sendTokenToBackend(token: String) {
        val url = "http://192.168.50.100:3000/guardar_token"

        val jsonBody = JSONObject()
        jsonBody.put("token", token)

        val requestBody = jsonBody.toString()

        val request = object : StringRequest(
            Request.Method.POST, url,
            { response -> Log.d(TAG, "Token enviado con éxito: $response") },
            { error -> Log.e(TAG, "Error enviando token: $error") }
        ) {
            override fun getBodyContentType() = "application/json; charset=utf-8"
            override fun getBody() = requestBody.toByteArray(Charsets.UTF_8)
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun sendNotification(messageTitle: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si no existe (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                channel = NotificationChannel(
                    channelId,
                    "Solicitudes de Locker",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $channelId")
            }
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_cainflockers_logo)  // Asegúrate que este ícono exista
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
        Log.d(TAG, "Notification sent")
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }

}

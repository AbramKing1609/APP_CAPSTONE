package com.example.app_capstone.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.app_capstone.MainActivity
import com.example.app_capstone.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val CHANNEL_ID = "medical_app_notifications"
        const val CHANNEL_NAME = "Medical App Notifications"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // 🔹 ESTA VERIFICACIÓN SÍ DEBE MANTENERSE (notificaciones push/FCM)
        if (!areNotificationsEnabledInApp()) {
            Log.d(TAG, "🔕 NOTIFICACIONES PUSH DESACTIVADAS - No se procesará mensaje FCM")
            return
        }

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "App Médica"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Nueva notificación"

        Log.d(TAG, "🔔 Procesando notificación FCM: $title")
        sendPersistentNotification(title, body, remoteMessage.data)
    }

    /**
     * 🔹 VERIFICACIÓN MÁS ROBUSTA DEL ESTADO DE NOTIFICACIONES
     */
    private fun areNotificationsEnabledInApp(): Boolean {
        return try {
            val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val enabled = sharedPreferences.getBoolean("notifications_enabled", true)
            Log.d(TAG, "🔔 FCM - Estado verificado: $enabled")

            // 🔹 VERIFICACIÓN EXTRA: Si no existe la preferencia, considerar como activada
            if (!sharedPreferences.contains("notifications_enabled")) {
                Log.d(TAG, "🔔 FCM - Preferencia no existe, usando true por defecto")
                true
            } else {
                enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar estado, usando true por defecto: ${e.message}")
            true
        }
    }
    /**
     * 🔹 NOTIFICACIÓN PERSISTENTE QUE NO DESAPARECE AL DESBLOQUEAR
     */
    private fun sendPersistentNotification(title: String, body: String, data: Map<String, String>) {
        try {
            // Intent directo a notificaciones
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK

                putExtra("OPEN_DIRECTLY_TO", "NOTIFICATIONS")
                putExtra("from_notification", true)
                putExtra("notification_title", title)
                putExtra("notification_body", body)

                action = "OPEN_NOTIFICATIONS_${System.currentTimeMillis()}"
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 🔹 CONFIGURACIÓN DE NOTIFICACIÓN ONGOING (PERSISTENTE)
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true) // Se elimina solo al hacer clic
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true) // 🔹 CRÍTICO: Hacerla ONGOING (persistente)
                .setTimeoutAfter(0) // 🔹 0 = NO se auto-elimina nunca
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary)) // Color personalizado

            // 🔹 AGREGAR BOTÓN PARA ELIMINAR MANUALMENTE
            val dismissIntent = Intent(this, NotificationDismissReceiver::class.java).apply {
                putExtra("notification_id", System.currentTimeMillis().toInt())
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                this,
                System.currentTimeMillis().toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.addAction(
                R.drawable.ic_notification,
                "Eliminar",
                dismissPendingIntent
            )

            // 🔹 VIBRACIÓN
            notificationBuilder.setVibrate(longArrayOf(1000, 800, 1000, 800))

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 🔹 CREAR CANAL PARA NOTIFICACIONES PERSISTENTES
            createPersistentNotificationChannel(notificationManager)

            // Notificar con ID único
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "✅ Notificación ONGOING enviada - ID: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en notificación ongoing: ${e.message}")
        }
    }

    /**
     * 🔹 CANAL DE NOTIFICACIÓN DE ALTA PERSISTENCIA
     */
    private fun createPersistentNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔹 CANAL PARA NOTIFICACIONES MÉDICAS URGENTES
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones médicas urgentes - No se eliminan automáticamente"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 800, 1000, 800, 1000)

                // 🔹 CONFIGURACIÓN MÁXIMA PARA PERSISTENCIA
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                setBypassDnd(true) // Ignorar "No molestar"

                // 🔹 PARA ANDROID 8.0+ - MÁXIMA VISIBILIDAD
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }

                // 🔹 MARCAR COMO CANAL IMPORTANTE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Configuraciones adicionales para Android O
                }
            }

            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ Canal de ALTA PERSISTENCIA creado")
        }
    }
}
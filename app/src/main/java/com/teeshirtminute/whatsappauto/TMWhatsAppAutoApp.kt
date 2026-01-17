package com.teeshirtminute.whatsappauto

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TMWhatsAppAutoApp : Application() {
    
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "tm_whatsapp_auto_service"
        const val NOTIFICATION_CHANNEL_MESSAGES = "tm_whatsapp_auto_messages"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Canal pour le service en arrière-plan
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)
            
            // Canal pour les messages envoyés
            val messagesChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_MESSAGES,
                "Messages envoyés",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications quand un message est envoyé"
            }
            notificationManager.createNotificationChannel(messagesChannel)
        }
    }
}

package com.teeshirtminute.whatsappauto.services

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.teeshirtminute.whatsappauto.R
import com.teeshirtminute.whatsappauto.TMWhatsAppAutoApp
import com.teeshirtminute.whatsappauto.data.*
import com.teeshirtminute.whatsappauto.ui.MainActivity
import com.teeshirtminute.whatsappauto.utils.PermissionUtils
import com.teeshirtminute.whatsappauto.utils.PhoneUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class MessageSenderService : Service() {
    
    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        private const val TAG = "MessageSenderService"
        private const val NOTIFICATION_ID_BASE = 2000
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefsManager: PreferencesManager
    private lateinit var historyManager: HistoryManager
    private val apiClient = ApiClient()
    
    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        historyManager = HistoryManager(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER)
        
        if (phoneNumber.isNullOrBlank()) {
            Log.e(TAG, "No phone number provided")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        
        scope.launch {
            sendAutoResponse(phoneNumber)
            stopSelf(startId)
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
    
    private suspend fun sendAutoResponse(phoneNumber: String) {
        Log.d(TAG, "Sending auto-response to: $phoneNumber")
        
        // Récupérer les settings
        val wordpressUrl = prefsManager.wordpressUrl.first()
        val apiKey = prefsManager.apiKey.first()
        
        if (wordpressUrl.isBlank()) {
            Log.e(TAG, "WordPress URL not configured")
            saveFailedAttempt(phoneNumber, "URL non configurée")
            return
        }
        
        // Appeler l'API
        when (val result = apiClient.getWhatsAppMessage(wordpressUrl, apiKey)) {
            is ApiClient.ApiResult.Success -> {
                val response = result.data
                
                Log.d(TAG, "API response - should_send: ${response.shouldSend}, status: ${response.storeStatus}")
                
                if (!response.shouldSend) {
                    Log.d(TAG, "Store is open, not sending message")
                    return
                }
                
                // Essayer WhatsApp Business d'abord
                val whatsAppSent = trySendWhatsApp(phoneNumber, response.message)
                
                if (whatsAppSent) {
                    Log.d(TAG, "WhatsApp message sent successfully")
                    saveSuccessfulAttempt(phoneNumber, MessageType.WHATSAPP, response.storeStatus)
                    showNotification(phoneNumber, MessageType.WHATSAPP)
                    prefsManager.incrementMessagesSent(phoneNumber)
                } else {
                    // Fallback SMS
                    Log.d(TAG, "WhatsApp failed, trying SMS...")
                    val smsSent = trySendSms(phoneNumber, response.messageSms)
                    
                    if (smsSent) {
                        Log.d(TAG, "SMS sent successfully")
                        saveSuccessfulAttempt(phoneNumber, MessageType.SMS, response.storeStatus)
                        showNotification(phoneNumber, MessageType.SMS)
                        prefsManager.incrementMessagesSent(phoneNumber)
                    } else {
                        Log.e(TAG, "Both WhatsApp and SMS failed")
                        saveFailedAttempt(phoneNumber, response.storeStatus)
                    }
                }
            }
            
            is ApiClient.ApiResult.Error -> {
                Log.e(TAG, "API error: ${result.message}")
                saveFailedAttempt(phoneNumber, "Erreur API: ${result.message}")
            }
        }
    }
    
    private fun trySendWhatsApp(phoneNumber: String, message: String): Boolean {
        if (!PermissionUtils.isWhatsAppBusinessInstalled(this)) {
            Log.d(TAG, "WhatsApp Business not installed")
            return false
        }
        
        return try {
            val whatsappNumber = PhoneUtils.toWhatsAppFormat(phoneNumber)
            val encodedMessage = Uri.encode(message)
            
            // Utiliser l'intent direct vers WhatsApp Business
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$whatsappNumber?text=$encodedMessage")
                setPackage("com.whatsapp.w4b")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            startActivity(intent)
            
            // Note: On ne peut pas vraiment savoir si le message a été envoyé
            // via cette méthode. Le service d'accessibilité sera utilisé pour
            // réellement envoyer le message.
            
            // Pour l'instant, on signale le succès car l'intent a été lancé
            // Le service d'accessibilité WhatsAppAccessibilityService
            // va détecter l'ouverture et cliquer sur envoyer
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhatsApp: ${e.message}")
            false
        }
    }
    
    private fun trySendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            
            // Diviser le message si trop long
            val parts = smsManager.divideMessage(message)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}")
            false
        }
    }
    
    private fun saveSuccessfulAttempt(phoneNumber: String, type: MessageType, storeStatus: String) {
        historyManager.addEntry(
            MessageHistoryItem(
                phoneNumber = phoneNumber,
                messageType = type,
                status = MessageStatus.SENT,
                storeStatus = storeStatus
            )
        )
    }
    
    private fun saveFailedAttempt(phoneNumber: String, storeStatus: String) {
        historyManager.addEntry(
            MessageHistoryItem(
                phoneNumber = phoneNumber,
                messageType = MessageType.WHATSAPP,
                status = MessageStatus.FAILED,
                storeStatus = storeStatus
            )
        )
    }
    
    private fun showNotification(phoneNumber: String, type: MessageType) {
        val formattedNumber = PhoneUtils.formatForDisplay(phoneNumber)
        val typeText = if (type == MessageType.WHATSAPP) "WhatsApp" else "SMS"
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, TMWhatsAppAutoApp.NOTIFICATION_CHANNEL_MESSAGES)
            .setContentTitle("Message envoyé via $typeText")
            .setContentText("Réponse auto envoyée à $formattedNumber")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(this)
                .notify(NOTIFICATION_ID_BASE + System.currentTimeMillis().toInt() % 1000, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted")
        }
    }
}

package com.teeshirtminute.whatsappauto.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.teeshirtminute.whatsappauto.data.PreferencesManager
import com.teeshirtminute.whatsappauto.services.MessageSenderService
import com.teeshirtminute.whatsappauto.utils.PhoneUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "CallReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var lastIncomingNumber: String? = null
        private var callStartTime: Long = 0
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }
        
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Phone state changed: $state, number: $phoneNumber")
        
        CoroutineScope(Dispatchers.IO).launch {
            val prefsManager = PreferencesManager(context)
            
            // Vérifier si le service est activé
            val isEnabled = prefsManager.serviceEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Service disabled, ignoring call")
                return@launch
            }
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Appel entrant
                    if (phoneNumber != null) {
                        lastIncomingNumber = phoneNumber
                        callStartTime = System.currentTimeMillis()
                        Log.d(TAG, "Incoming call from: $phoneNumber")
                    }
                }
                
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Appel terminé
                    if (lastState == TelephonyManager.CALL_STATE_RINGING && lastIncomingNumber != null) {
                        // L'appel a sonné mais n'a pas été décroché = appel manqué
                        val callDuration = System.currentTimeMillis() - callStartTime
                        
                        // Considérer comme manqué si moins de 2 secondes (pas décroché)
                        if (callDuration < 30000) { // 30 secondes max
                            handleMissedCall(context, lastIncomingNumber!!)
                        }
                    }
                    lastIncomingNumber = null
                    callStartTime = 0
                }
                
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Appel décroché - ne rien faire
                    Log.d(TAG, "Call answered, not a missed call")
                }
            }
            
            lastState = when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                else -> TelephonyManager.CALL_STATE_IDLE
            }
        }
    }
    
    private fun handleMissedCall(context: Context, phoneNumber: String) {
        Log.d(TAG, "Handling missed call from: $phoneNumber")
        
        // Vérifier si c'est un numéro mobile français (06 ou 07)
        if (!PhoneUtils.isFrenchMobile(phoneNumber)) {
            Log.d(TAG, "Not a French mobile number, ignoring: $phoneNumber")
            return
        }
        
        // Vérifier si c'est un numéro fixe
        if (PhoneUtils.isFixedLine(phoneNumber)) {
            Log.d(TAG, "Fixed line number, ignoring: $phoneNumber")
            return
        }
        
        Log.d(TAG, "French mobile detected, sending auto-response to: $phoneNumber")
        
        // Lancer le service d'envoi de message
        val serviceIntent = Intent(context, MessageSenderService::class.java).apply {
            putExtra(MessageSenderService.EXTRA_PHONE_NUMBER, phoneNumber)
        }
        
        context.startService(serviceIntent)
    }
}

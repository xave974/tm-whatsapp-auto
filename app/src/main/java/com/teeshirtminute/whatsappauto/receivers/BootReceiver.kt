package com.teeshirtminute.whatsappauto.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.teeshirtminute.whatsappauto.data.PreferencesManager
import com.teeshirtminute.whatsappauto.services.CallMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking if service should start")
            
            CoroutineScope(Dispatchers.IO).launch {
                val prefsManager = PreferencesManager(context)
                val isEnabled = prefsManager.serviceEnabled.first()
                
                if (isEnabled) {
                    Log.d(TAG, "Service was enabled, restarting...")
                    CallMonitorService.start(context)
                } else {
                    Log.d(TAG, "Service was not enabled, skipping")
                }
            }
        }
    }
}

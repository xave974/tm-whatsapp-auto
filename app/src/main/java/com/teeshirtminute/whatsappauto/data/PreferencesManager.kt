package com.teeshirtminute.whatsappauto.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tm_settings")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val KEY_WORDPRESS_URL = stringPreferencesKey("wordpress_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_SIM_SLOT = intPreferencesKey("sim_slot") // 0 = SIM1, 1 = SIM2, -1 = both
        private val KEY_MESSAGES_SENT_TODAY = intPreferencesKey("messages_sent_today")
        private val KEY_LAST_MESSAGE_DATE = stringPreferencesKey("last_message_date")
        private val KEY_LAST_MESSAGE_PHONE = stringPreferencesKey("last_message_phone")
        private val KEY_LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    }
    
    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVICE_ENABLED] ?: false
    }
    
    val wordpressUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_WORDPRESS_URL] ?: ""
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }
    
    val simSlot: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SIM_SLOT] ?: 1 // Par défaut SIM 2
    }
    
    val messagesSentToday: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_MESSAGES_SENT_TODAY] ?: 0
    }
    
    val lastMessagePhone: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_MESSAGE_PHONE] ?: ""
    }
    
    val lastMessageDate: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_MESSAGE_DATE] ?: ""
    }
    
    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE_ENABLED] = enabled
        }
    }
    
    suspend fun setWordpressUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WORDPRESS_URL] = url.trimEnd('/')
        }
    }
    
    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key
        }
    }
    
    suspend fun setSimSlot(slot: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SIM_SLOT] = slot
        }
    }
    
    suspend fun incrementMessagesSent(phoneNumber: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val now = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        context.dataStore.edit { prefs ->
            val lastReset = prefs[KEY_LAST_RESET_DATE] ?: ""
            
            // Reset counter if new day
            if (lastReset != today) {
                prefs[KEY_MESSAGES_SENT_TODAY] = 1
                prefs[KEY_LAST_RESET_DATE] = today
            } else {
                val current = prefs[KEY_MESSAGES_SENT_TODAY] ?: 0
                prefs[KEY_MESSAGES_SENT_TODAY] = current + 1
            }
            
            prefs[KEY_LAST_MESSAGE_PHONE] = phoneNumber
            prefs[KEY_LAST_MESSAGE_DATE] = now
        }
    }
    
    suspend fun getWordpressUrlSync(): String {
        var url = ""
        context.dataStore.edit { prefs ->
            url = prefs[KEY_WORDPRESS_URL] ?: ""
        }
        return url
    }
    
    suspend fun getApiKeySync(): String {
        var key = ""
        context.dataStore.edit { prefs ->
            key = prefs[KEY_API_KEY] ?: ""
        }
        return key
    }
}

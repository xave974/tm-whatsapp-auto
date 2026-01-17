package com.teeshirtminute.whatsappauto.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("tm_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_HISTORY = "message_history"
        private const val MAX_HISTORY_ITEMS = 100
    }
    
    fun addEntry(item: MessageHistoryItem) {
        val history = getHistory().toMutableList()
        history.add(0, item) // Add at beginning
        
        // Keep only last MAX_HISTORY_ITEMS
        val trimmedHistory = history.take(MAX_HISTORY_ITEMS)
        
        saveHistory(trimmedHistory)
    }
    
    fun getHistory(): List<MessageHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MessageHistoryItem>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getTodayCount(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        return getHistory().count { item ->
            val itemDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(item.timestamp))
            itemDate == today
        }
    }
    
    fun getLastEntry(): MessageHistoryItem? {
        return getHistory().firstOrNull()
    }
    
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    private fun saveHistory(history: List<MessageHistoryItem>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}

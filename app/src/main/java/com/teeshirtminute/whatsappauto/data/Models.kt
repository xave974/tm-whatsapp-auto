package com.teeshirtminute.whatsappauto.data

import com.google.gson.annotations.SerializedName

data class WhatsAppMessageResponse(
    @SerializedName("should_send")
    val shouldSend: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("message_sms")
    val messageSms: String,
    
    @SerializedName("store_status")
    val storeStatus: String
)

data class StoreStatusResponse(
    @SerializedName("is_open")
    val isOpen: Boolean,
    
    @SerializedName("current_status")
    val currentStatus: String,
    
    @SerializedName("current_time")
    val currentTime: String,
    
    @SerializedName("current_day_name")
    val currentDayName: String,
    
    @SerializedName("next_opening_formatted")
    val nextOpeningFormatted: String?,
    
    @SerializedName("express_date_formatted")
    val expressDateFormatted: String?,
    
    @SerializedName("store_hours")
    val storeHours: String,
    
    @SerializedName("store_address")
    val storeAddress: String,
    
    @SerializedName("whatsapp_message")
    val whatsappMessage: String,
    
    @SerializedName("sms_message")
    val smsMessage: String
)

data class MessageHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType,
    val status: MessageStatus,
    val storeStatus: String
)

enum class MessageType {
    WHATSAPP,
    SMS
}

enum class MessageStatus {
    SENT,
    FAILED,
    PENDING
}

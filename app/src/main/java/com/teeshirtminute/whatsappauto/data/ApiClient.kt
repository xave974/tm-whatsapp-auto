package com.teeshirtminute.whatsappauto.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ApiClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    }
    
    suspend fun getWhatsAppMessage(baseUrl: String, apiKey: String? = null): ApiResult<WhatsAppMessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/wp-json/tm-pdv/v1/whatsapp-message"
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                
                if (!apiKey.isNullOrBlank()) {
                    requestBuilder.addHeader("X-API-Key", apiKey)
                }
                
                val response = client.newCall(requestBuilder.build()).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val data = gson.fromJson(body, WhatsAppMessageResponse::class.java)
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Error("Réponse vide", response.code)
                    }
                } else {
                    ApiResult.Error("Erreur serveur: ${response.code}", response.code)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
    
    suspend fun getStoreStatus(baseUrl: String, apiKey: String? = null): ApiResult<StoreStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$baseUrl/wp-json/tm-pdv/v1/store-status"
                
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                
                if (!apiKey.isNullOrBlank()) {
                    requestBuilder.addHeader("X-API-Key", apiKey)
                }
                
                val response = client.newCall(requestBuilder.build()).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val data = gson.fromJson(body, StoreStatusResponse::class.java)
                        ApiResult.Success(data)
                    } else {
                        ApiResult.Error("Réponse vide", response.code)
                    }
                } else {
                    ApiResult.Error("Erreur serveur: ${response.code}", response.code)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Erreur inconnue")
            }
        }
    }
    
    suspend fun testConnection(baseUrl: String, apiKey: String? = null): Boolean {
        return when (getStoreStatus(baseUrl, apiKey)) {
            is ApiResult.Success -> true
            is ApiResult.Error -> false
        }
    }
}

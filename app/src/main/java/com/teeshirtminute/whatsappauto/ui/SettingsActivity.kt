package com.teeshirtminute.whatsappauto.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.teeshirtminute.whatsappauto.R
import com.teeshirtminute.whatsappauto.data.ApiClient
import com.teeshirtminute.whatsappauto.data.PreferencesManager
import com.teeshirtminute.whatsappauto.databinding.ActivitySettingsBinding
import com.teeshirtminute.whatsappauto.utils.PermissionUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsManager: PreferencesManager
    private val apiClient = ApiClient()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager(this)
        
        setupToolbar()
        loadSettings()
        setupListeners()
        checkWhatsAppStatus()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            binding.etWordpressUrl.setText(prefsManager.wordpressUrl.first())
            binding.etApiKey.setText(prefsManager.apiKey.first())
            
            when (prefsManager.simSlot.first()) {
                0 -> binding.rbSim1.isChecked = true
                1 -> binding.rbSim2.isChecked = true
                else -> binding.rbBothSims.isChecked = true
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun testConnection() {
        val url = binding.etWordpressUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        
        if (url.isBlank()) {
            binding.tvConnectionStatus.text = "❌ Veuillez entrer l'URL"
            binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_error))
            binding.tvConnectionStatus.visibility = View.VISIBLE
            return
        }
        
        binding.btnTestConnection.isEnabled = false
        binding.tvConnectionStatus.text = "⏳ Test en cours..."
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        binding.tvConnectionStatus.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val success = apiClient.testConnection(url, apiKey.ifBlank { null })
            
            binding.btnTestConnection.isEnabled = true
            
            if (success) {
                binding.tvConnectionStatus.text = "✅ Connexion réussie !"
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.status_open))
            } else {
                binding.tvConnectionStatus.text = "❌ Échec de connexion. Vérifiez l'URL et la clé API."
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.status_error))
            }
        }
    }
    
    private fun saveSettings() {
        val url = binding.etWordpressUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        
        val simSlot = when {
            binding.rbSim1.isChecked -> 0
            binding.rbSim2.isChecked -> 1
            else -> -1
        }
        
        lifecycleScope.launch {
            prefsManager.setWordpressUrl(url)
            prefsManager.setApiKey(apiKey)
            prefsManager.setSimSlot(simSlot)
            
            Toast.makeText(this@SettingsActivity, "Paramètres enregistrés", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun checkWhatsAppStatus() {
        val hasWhatsAppBusiness = PermissionUtils.isWhatsAppBusinessInstalled(this)
        val hasWhatsApp = PermissionUtils.isWhatsAppInstalled(this)
        
        when {
            hasWhatsAppBusiness -> {
                binding.tvWhatsAppStatus.text = "✅ WhatsApp Business installé"
                binding.ivWhatsAppStatus.setImageResource(R.drawable.ic_check)
                binding.ivWhatsAppStatus.setColorFilter(ContextCompat.getColor(this, R.color.status_open))
            }
            hasWhatsApp -> {
                binding.tvWhatsAppStatus.text = "⚠️ WhatsApp standard installé (pas Business)"
                binding.ivWhatsAppStatus.setImageResource(R.drawable.ic_close)
                binding.ivWhatsAppStatus.setColorFilter(ContextCompat.getColor(this, R.color.status_closed))
            }
            else -> {
                binding.tvWhatsAppStatus.text = "❌ WhatsApp non installé"
                binding.ivWhatsAppStatus.setImageResource(R.drawable.ic_close)
                binding.ivWhatsAppStatus.setColorFilter(ContextCompat.getColor(this, R.color.status_error))
            }
        }
    }
}

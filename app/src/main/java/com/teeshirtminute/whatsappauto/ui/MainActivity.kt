package com.teeshirtminute.whatsappauto.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.teeshirtminute.whatsappauto.R
import com.teeshirtminute.whatsappauto.data.ApiClient
import com.teeshirtminute.whatsappauto.data.HistoryManager
import com.teeshirtminute.whatsappauto.data.PreferencesManager
import com.teeshirtminute.whatsappauto.databinding.ActivityMainBinding
import com.teeshirtminute.whatsappauto.services.CallMonitorService
import com.teeshirtminute.whatsappauto.utils.PermissionUtils
import com.teeshirtminute.whatsappauto.utils.PhoneUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var historyManager: HistoryManager
    private val apiClient = ApiClient()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { 
        updatePermissionsUI()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefsManager = PreferencesManager(this)
        historyManager = HistoryManager(this)
        
        setupToolbar()
        setupListeners()
        observePreferences()
    }
    
    override fun onResume() {
        super.onResume()
        updatePermissionsUI()
        updateStatsUI()
        refreshStoreStatus()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupListeners() {
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefsManager.setServiceEnabled(isChecked)
                
                if (isChecked) {
                    if (PermissionUtils.hasAllRequiredPermissions(this@MainActivity)) {
                        CallMonitorService.start(this@MainActivity)
                    } else {
                        requestPermissions()
                    }
                } else {
                    CallMonitorService.stop(this@MainActivity)
                }
                
                updateServiceUI(isChecked)
            }
        }
        
        binding.btnGrantPermissions.setOnClickListener {
            requestPermissions()
        }
        
        binding.layoutPermAccessibility.setOnClickListener {
            PermissionUtils.openAccessibilitySettings(this)
        }
        
        binding.btnRefreshStatus.setOnClickListener {
            refreshStoreStatus()
        }
        
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }
    
    private fun observePreferences() {
        lifecycleScope.launch {
            prefsManager.serviceEnabled.collect { enabled ->
                binding.switchService.isChecked = enabled
                updateServiceUI(enabled)
            }
        }
    }
    
    private fun updateServiceUI(enabled: Boolean) {
        binding.tvServiceStatus.text = if (enabled) {
            getString(R.string.service_enabled)
        } else {
            getString(R.string.service_disabled)
        }
        
        binding.viewStatusIndicator.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (enabled) R.color.status_open else R.color.status_error
            )
        )
    }
    
    private fun updatePermissionsUI() {
        val hasPhone = PermissionUtils.hasPhonePermissions(this)
        val hasSms = PermissionUtils.hasSmsPermission(this)
        val hasAccessibility = PermissionUtils.isAccessibilityServiceEnabled(this)
        
        updatePermissionIcon(binding.ivPermPhone, hasPhone)
        updatePermissionIcon(binding.ivPermSms, hasSms)
        updatePermissionIcon(binding.ivPermAccessibility, hasAccessibility)
        
        val allGranted = hasPhone && hasSms && hasAccessibility
        binding.btnGrantPermissions.visibility = if (allGranted) View.GONE else View.VISIBLE
    }
    
    private fun updatePermissionIcon(imageView: android.widget.ImageView, granted: Boolean) {
        if (granted) {
            imageView.setImageResource(R.drawable.ic_check)
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.status_open))
        } else {
            imageView.setImageResource(R.drawable.ic_close)
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.status_error))
        }
    }
    
    private fun updateStatsUI() {
        val todayCount = historyManager.getTodayCount()
        binding.tvMessageCount.text = todayCount.toString()
        
        val lastEntry = historyManager.getLastEntry()
        if (lastEntry != null) {
            binding.tvLastMessagePhone.text = PhoneUtils.formatForDisplay(lastEntry.phoneNumber)
            val dateFormat = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
            binding.tvLastMessageTime.text = dateFormat.format(java.util.Date(lastEntry.timestamp))
        } else {
            binding.tvLastMessagePhone.text = "--"
            binding.tvLastMessageTime.text = getString(R.string.no_message_yet)
        }
    }
    
    private fun refreshStoreStatus() {
        binding.tvStoreStatus.text = "Chargement..."
        binding.tvNextOpening.visibility = View.GONE
        
        lifecycleScope.launch {
            val url = prefsManager.wordpressUrl.first()
            val apiKey = prefsManager.apiKey.first()
            
            if (url.isBlank()) {
                binding.tvStoreStatus.text = "⚠️ URL non configurée"
                binding.tvStoreStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_closed))
                return@launch
            }
            
            when (val result = apiClient.getStoreStatus(url, apiKey)) {
                is ApiClient.ApiResult.Success -> {
                    val status = result.data
                    
                    if (status.isOpen) {
                        binding.tvStoreStatus.text = "🟢 Ouverte"
                        binding.tvStoreStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_open))
                    } else {
                        binding.tvStoreStatus.text = "🟡 Fermée"
                        binding.tvStoreStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_closed))
                    }
                    
                    if (!status.nextOpeningFormatted.isNullOrBlank()) {
                        binding.tvNextOpening.text = "Réouverture : ${status.nextOpeningFormatted}"
                        binding.tvNextOpening.visibility = View.VISIBLE
                    }
                }
                
                is ApiClient.ApiResult.Error -> {
                    binding.tvStoreStatus.text = "❌ Erreur de connexion"
                    binding.tvStoreStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_error))
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
        
        if (!PermissionUtils.isAccessibilityServiceEnabled(this)) {
            AlertDialog.Builder(this)
                .setTitle("Accessibilité requise")
                .setMessage("Pour envoyer des messages WhatsApp automatiquement, vous devez activer le service d'accessibilité.\n\nVoulez-vous ouvrir les paramètres ?")
                .setPositiveButton("Ouvrir") { _, _ ->
                    PermissionUtils.openAccessibilitySettings(this)
                }
                .setNegativeButton("Plus tard", null)
                .show()
        }
    }
}

package com.teeshirtminute.whatsappauto.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teeshirtminute.whatsappauto.R
import com.teeshirtminute.whatsappauto.data.HistoryManager
import com.teeshirtminute.whatsappauto.data.MessageHistoryItem
import com.teeshirtminute.whatsappauto.data.MessageStatus
import com.teeshirtminute.whatsappauto.data.MessageType
import com.teeshirtminute.whatsappauto.databinding.ActivityHistoryBinding
import com.teeshirtminute.whatsappauto.databinding.ItemHistoryBinding
import com.teeshirtminute.whatsappauto.utils.PhoneUtils
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyManager: HistoryManager
    private lateinit var adapter: HistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        historyManager = HistoryManager(this)
        
        setupToolbar()
        setupRecyclerView()
        loadHistory()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = HistoryAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun loadHistory() {
        val history = historyManager.getHistory()
        
        if (history.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            adapter.submitList(history)
        }
    }
    
    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        
        private var items: List<MessageHistoryItem> = emptyList()
        
        fun submitList(newItems: List<MessageHistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHistoryBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
            
            private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            fun bind(item: MessageHistoryItem) {
                binding.tvPhoneNumber.text = PhoneUtils.formatForDisplay(item.phoneNumber)
                binding.tvDateTime.text = dateFormat.format(Date(item.timestamp))
                
                // Type de message
                when (item.messageType) {
                    MessageType.WHATSAPP -> {
                        binding.ivMessageType.setImageResource(R.drawable.ic_whatsapp)
                    }
                    MessageType.SMS -> {
                        binding.ivMessageType.setImageResource(R.drawable.ic_sms)
                    }
                }
                
                // Statut
                when (item.status) {
                    MessageStatus.SENT -> {
                        val typeText = if (item.messageType == MessageType.WHATSAPP) "WhatsApp" else "SMS"
                        binding.tvStatus.text = "✓ Envoyé via $typeText"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.status_open))
                        binding.ivStatus.setImageResource(R.drawable.ic_check)
                        binding.ivStatus.setColorFilter(ContextCompat.getColor(this@HistoryActivity, R.color.status_open))
                    }
                    MessageStatus.FAILED -> {
                        binding.tvStatus.text = "✗ Échec d'envoi"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.status_error))
                        binding.ivStatus.setImageResource(R.drawable.ic_close)
                        binding.ivStatus.setColorFilter(ContextCompat.getColor(this@HistoryActivity, R.color.status_error))
                    }
                    MessageStatus.PENDING -> {
                        binding.tvStatus.text = "⏳ En cours..."
                        binding.tvStatus.setTextColor(ContextCompat.getColor(this@HistoryActivity, R.color.status_closed))
                        binding.ivStatus.setImageResource(R.drawable.ic_check)
                        binding.ivStatus.setColorFilter(ContextCompat.getColor(this@HistoryActivity, R.color.status_closed))
                    }
                }
            }
        }
    }
}

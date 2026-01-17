package com.teeshirtminute.whatsappauto.services

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "WhatsAppAccessibility"
        private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        
        // État partagé pour savoir si on attend d'envoyer un message
        @Volatile
        var pendingMessage: Boolean = false
        
        @Volatile
        var lastMessageTime: Long = 0
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Vérifier si c'est WhatsApp Business
        if (event.packageName?.toString() != WHATSAPP_BUSINESS_PACKAGE) {
            return
        }
        
        // Vérifier si on a un message en attente d'envoi (moins de 10 secondes)
        if (!pendingMessage && (System.currentTimeMillis() - lastMessageTime) > 10000) {
            return
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Délai pour laisser l'UI se charger
                handler.postDelayed({
                    tryClickSendButton()
                }, 500)
            }
        }
    }
    
    private fun tryClickSendButton() {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // Chercher le bouton d'envoi de WhatsApp Business
            val sendButtons = mutableListOf<AccessibilityNodeInfo>()
            
            // Méthode 1: Chercher par description
            findNodesByDescription(rootNode, listOf("envoyer", "send"), sendButtons)
            
            // Méthode 2: Chercher par ID (WhatsApp Business)
            findNodeById(rootNode, "com.whatsapp.w4b:id/send", sendButtons)
            
            // Méthode 3: Chercher l'ImageButton d'envoi
            if (sendButtons.isEmpty()) {
                findSendImageButton(rootNode, sendButtons)
            }
            
            // Cliquer sur le premier bouton trouvé
            for (button in sendButtons) {
                if (button.isClickable && button.isEnabled) {
                    Log.d(TAG, "Found send button, clicking...")
                    
                    if (button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        Log.d(TAG, "Send button clicked successfully")
                        pendingMessage = false
                        lastMessageTime = 0
                        
                        // Fermer WhatsApp après un délai
                        handler.postDelayed({
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }, 1000)
                        
                        return
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finding/clicking send button: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }
    
    private fun findNodesByDescription(
        node: AccessibilityNodeInfo,
        descriptions: List<String>,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        for (desc in descriptions) {
            if (contentDesc.contains(desc.lowercase())) {
                results.add(node)
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByDescription(child, descriptions, results)
        }
    }
    
    private fun findNodeById(
        node: AccessibilityNodeInfo,
        viewId: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.viewIdResourceName == viewId) {
            results.add(node)
        }
        
        val foundNodes = node.findAccessibilityNodeInfosByViewId(viewId)
        results.addAll(foundNodes)
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodeById(child, viewId, results)
        }
    }
    
    private fun findSendImageButton(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.toString() == "android.widget.ImageButton" ||
            node.className?.toString() == "android.widget.ImageView") {
            
            if (node.isClickable) {
                val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
                if (contentDesc.contains("send") || 
                    contentDesc.contains("envoyer") ||
                    contentDesc.isEmpty()) {
                    results.add(node)
                }
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findSendImageButton(child, results)
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }
}

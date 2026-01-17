package com.teeshirtminute.whatsappauto.utils

object PhoneUtils {
    
    /**
     * Vérifie si c'est un numéro mobile français (06 ou 07)
     */
    fun isFrenchMobile(phoneNumber: String): Boolean {
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        // Format français : 06xxxxxxxx ou 07xxxxxxxx
        if (cleaned.matches(Regex("^0[67]\\d{8}$"))) {
            return true
        }
        
        // Format international : +336xxxxxxxx ou +337xxxxxxxx
        if (cleaned.matches(Regex("^\\+?33[67]\\d{8}$"))) {
            return true
        }
        
        // Format avec 00 : 00336xxxxxxxx
        if (cleaned.matches(Regex("^0033[67]\\d{8}$"))) {
            return true
        }
        
        return false
    }
    
    /**
     * Vérifie si c'est un numéro fixe (à ignorer)
     */
    fun isFixedLine(phoneNumber: String): Boolean {
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        // Numéros commençant par 01, 02, 03, 04, 05, 08, 09
        if (cleaned.matches(Regex("^0[1-5]\\d{8}$"))) {
            return true
        }
        if (cleaned.matches(Regex("^0[89]\\d{8}$"))) {
            return true
        }
        
        return false
    }
    
    /**
     * Nettoie un numéro de téléphone (supprime espaces, tirets, etc.)
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[\\s.\\-()]"), "")
    }
    
    /**
     * Formate un numéro pour l'affichage
     */
    fun formatForDisplay(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        // Format français standard
        if (cleaned.matches(Regex("^0[0-9]{9}$"))) {
            return cleaned.chunked(2).joinToString(" ")
        }
        
        // Format international
        if (cleaned.startsWith("+33") && cleaned.length == 12) {
            val national = "0" + cleaned.substring(3)
            return national.chunked(2).joinToString(" ")
        }
        
        return phoneNumber
    }
    
    /**
     * Convertit en format international pour WhatsApp
     */
    fun toInternationalFormat(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        // Déjà en format international
        if (cleaned.startsWith("+33")) {
            return cleaned
        }
        
        // Format avec 0033
        if (cleaned.startsWith("0033")) {
            return "+" + cleaned.substring(2)
        }
        
        // Format national français
        if (cleaned.startsWith("0") && cleaned.length == 10) {
            return "+33" + cleaned.substring(1)
        }
        
        return phoneNumber
    }
    
    /**
     * Extrait le numéro sans préfixe international (pour WhatsApp)
     */
    fun toWhatsAppFormat(phoneNumber: String): String {
        val international = toInternationalFormat(phoneNumber)
        // WhatsApp utilise le format sans le +
        return international.removePrefix("+")
    }
}

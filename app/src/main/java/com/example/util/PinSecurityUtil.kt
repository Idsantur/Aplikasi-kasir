package com.example.util

import java.security.MessageDigest

object PinSecurityUtil {

    /**
     * Hashes a 6-digit PIN using SHA-256 with an internal salt to protect store credentials.
     * Produces a 64-character lowercase hexadecimal hash.
     */
    fun hashPin(pin: String): String {
        val salted = "KasirKilat_Store_Salt_#2026_$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Normalizes phone number into standard 08... format.
     * Handles inputs like "+62 812-3456-7890", "628123456789", "0812-3456-7890".
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        var clean = rawPhone.replace(Regex("[^0-9+]"), "").trim()
        if (clean.startsWith("+62")) {
            clean = "0" + clean.substring(3)
        } else if (clean.startsWith("62")) {
            clean = "0" + clean.substring(2)
        }
        return clean
    }

    /**
     * Validates Indonesian mobile phone number:
     * Must start with 08 and have between 10 and 14 digits.
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val clean = normalizePhoneNumber(phone)
        return clean.startsWith("08") && clean.length in 10..15 && clean.all { it.isDigit() }
    }

    /**
     * Validates PIN: Must be exactly 6 numeric digits.
     */
    fun isValidPin(pin: String): Boolean {
        return pin.length == 6 && pin.all { it.isDigit() }
    }
}

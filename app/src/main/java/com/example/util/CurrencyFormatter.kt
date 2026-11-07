package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {
    private val indonesianLocale = Locale("in", "ID")
    private val numberFormat = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return try {
            val formatted = numberFormat.format(amount)
            // Replace non-breaking space if any
            formatted.replace("Rp", "Rp ").replace("\u00A0", " ")
        } catch (e: Exception) {
            "Rp $amount"
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun generateInvoiceNumber(): String {
        val sdf = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        val randomSuffix = (10..99).random()
        return "INV-${sdf.format(Date())}$randomSuffix"
    }
}

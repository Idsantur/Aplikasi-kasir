package com.example

import com.example.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosCalculationUnitTest {

    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatRupiah(15000L)
        assertTrue("Format must include 15.000", formatted.contains("15.000"))
    }

    @Test
    fun testInvoiceGeneration() {
        val invoice = CurrencyFormatter.generateInvoiceNumber()
        assertTrue("Invoice must start with INV-", invoice.startsWith("INV-"))
    }

    @Test
    fun testProfitCalculation() {
        val costPrice = 8000L
        val sellingPrice = 15000L
        val profit = sellingPrice - costPrice
        assertEquals(7000L, profit)
    }

    @Test
    fun testChangeCalculation() {
        val total = 45000L
        val cashPaid = 50000L
        val change = cashPaid - total
        assertEquals(5000L, change)
    }
}

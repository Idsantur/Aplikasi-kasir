package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotal: Long,
    val discountAmount: Long = 0L,
    val taxAmount: Long = 0L,
    val serviceAmount: Long = 0L,
    val grandTotal: Long,
    val totalCost: Long = 0L,
    val netProfit: Long = 0L,
    val paymentMethod: String = "TUNAI",
    val amountPaid: Long = 0L,
    val changeAmount: Long = 0L,
    val customerName: String = "",
    val customerNote: String = ""
)

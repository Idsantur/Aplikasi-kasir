package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "KasirKilat Store",
    val phone: String = "0812-3456-7890",
    val address: String = "Jl. Merdeka No. 10, Jakarta",
    val receiptHeaderNote: String = "Selamat Berbelanja",
    val receiptFooterNote: String = "Terima kasih atas kunjungan Anda!\nBarang yang dibeli tidak dapat ditukar.",
    val taxPercent: Double = 0.0,
    val isTaxEnabled: Boolean = false
)

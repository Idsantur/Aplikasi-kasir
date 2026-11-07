package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "Umum",
    val costPrice: Long = 0L,
    val sellingPrice: Long = 0L,
    val stock: Int = 0,
    val minStockAlert: Int = 5,
    val unit: String = "pcs",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

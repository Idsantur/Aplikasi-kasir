package com.example.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudStoreAccount(
    @DocumentId val id: String = "",
    val phoneNumber: String = "",
    val pinHash: String = "",
    val storeName: String = "",
    val ownerUid: String = "",
    val address: String = "",
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudProductItem(
    @DocumentId val id: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val barcode: String = "",
    val category: String = "Umum",
    val sellingPrice: Long = 0L,
    val costPrice: Long = 0L,
    val stock: Int = 0,
    val unit: String = "pcs",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudCategoryItem(
    @DocumentId val id: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class RestoredStoreCatalog(
    val storeName: String,
    val address: String,
    val phone: String,
    val products: List<CloudProductItem>,
    val categories: List<String>
)

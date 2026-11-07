package com.example.ui.model

import com.example.data.local.entity.Product

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Long get() = product.sellingPrice * quantity
    val costTotal: Long get() = product.costPrice * quantity
}

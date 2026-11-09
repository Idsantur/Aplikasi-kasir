package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testProductCategoryUpdate() {
    val initialProduct = com.example.data.local.entity.Product(
      id = 1,
      name = "Kemeja Lengan Panjang",
      category = "Pakaian",
      sellingPrice = 75000,
      costPrice = 45000,
      stock = 20
    )
    val updated = initialProduct.copy(category = "Atasan")
    assertEquals("Atasan", updated.category)
    assertEquals(75000, updated.sellingPrice)
  }

  @Test
  fun testCategoryFallback() {
    val deletedCategoryName = "Lama"
    val productCategory = "Lama"
    val reassigned = if (productCategory.equals(deletedCategoryName, ignoreCase = true)) "Umum" else productCategory
    assertEquals("Umum", reassigned)
  }

  @Test
  fun testCartAutomaticSubtotalCalculation() {
    val shirt = com.example.data.local.entity.Product(
      id = 1,
      name = "Kemeja Kasual",
      category = "Pakaian",
      sellingPrice = 10000,
      costPrice = 6000,
      stock = 50
    )
    val cap = com.example.data.local.entity.Product(
      id = 2,
      name = "Topi Baseball",
      category = "Aksesoris",
      sellingPrice = 5000,
      costPrice = 2000,
      stock = 50
    )

    val item1 = com.example.ui.model.CartItem(product = shirt, quantity = 3)
    val item2 = com.example.ui.model.CartItem(product = cap, quantity = 2)

    assertEquals(30000L, item1.subtotal)
    assertEquals(10000L, item2.subtotal)

    val cartItems = listOf(item1, item2)
    val totalItems = cartItems.sumOf { it.quantity }
    val totalSubtotal = cartItems.sumOf { it.subtotal }

    assertEquals(5, totalItems)
    assertEquals(40000L, totalSubtotal)

    // Test with discount and change
    val discount = 5000L
    val grandTotal = (totalSubtotal - discount).coerceAtLeast(0L)
    assertEquals(35000L, grandTotal)

    val cashPaid = 50000L
    val change = cashPaid - grandTotal
    assertEquals(15000L, change)
  }

  @Test
  fun testUnwantedGroceryCategoryFilter() {
    val sampleCategories = listOf("Makanan", "Pakaian", "Minuman", "Elektronik", "Cemilan", "Snack", "Sembako", "Jasa", "Aksesoris")
    val unwanted = setOf("makanan", "minuman", "cemilan", "snak", "snack", "sembako", "jasa")
    val cleanCategories = sampleCategories.filter { !unwanted.contains(it.lowercase()) }

    assertEquals(listOf("Pakaian", "Elektronik", "Aksesoris"), cleanCategories)
  }
}

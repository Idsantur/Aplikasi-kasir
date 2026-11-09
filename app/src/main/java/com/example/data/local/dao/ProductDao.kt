package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT DISTINCT category FROM products WHERE isActive = 1 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE isActive = 1")
    suspend fun getAllActiveProductsList(): List<Product>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int)

    @Query("UPDATE products SET stock = MAX(0, stock + :delta) WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Int)

    @Query("SELECT SUM(costPrice * stock) FROM products WHERE isActive = 1")
    fun getTotalInventoryCost(): Flow<Long?>

    @Query("SELECT SUM(sellingPrice * stock) FROM products WHERE isActive = 1")
    fun getTotalInventoryValue(): Flow<Long?>

    @Query("SELECT SUM(stock) FROM products WHERE isActive = 1")
    fun getTotalStockUnits(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int
}

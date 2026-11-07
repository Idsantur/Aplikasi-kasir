package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT SUM(grandTotal) FROM transactions")
    fun getTotalRevenue(): Flow<Long?>

    @Query("SELECT SUM(netProfit) FROM transactions")
    fun getTotalProfit(): Flow<Long?>

    @Query("SELECT productId, productName, category, SUM(quantity) as totalQty, SUM(itemSubtotal) as totalSales FROM transaction_items GROUP BY productId ORDER BY totalQty DESC LIMIT :limit")
    fun getTopSellingProducts(limit: Int = 5): Flow<List<TopProductRaw>>
}

data class TopProductRaw(
    val productId: Long,
    val productName: String,
    val category: String,
    val totalQty: Int,
    val totalSales: Long
)

package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.TopProductRaw
import com.example.data.local.entity.Product
import com.example.data.local.entity.StoreProfile
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.model.TransactionWithItems
import kotlinx.coroutines.flow.Flow

class PosRepository(private val database: AppDatabase) {

    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val storeProfileDao = database.storeProfileDao()

    val allProducts: Flow<List<Product>> = productDao.getAllActiveProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
    val storeProfile: Flow<StoreProfile?> = storeProfileDao.getStoreProfile()
    val totalRevenue: Flow<Long?> = transactionDao.getTotalRevenue()
    val totalProfit: Flow<Long?> = transactionDao.getTotalProfit()
    val totalTransactionCount: Flow<Int> = transactionDao.getTransactionCount()
    val topSellingProducts: Flow<List<TopProductRaw>> = transactionDao.getTopSellingProducts(5)

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query)
    }

    suspend fun insertProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun updateStoreProfile(profile: StoreProfile) {
        storeProfileDao.insertOrUpdate(profile)
    }

    suspend fun checkout(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        return database.withTransaction {
            val transactionId = transactionDao.insertTransaction(transaction)
            val mappedItems = items.map { it.copy(transactionId = transactionId) }
            transactionDao.insertTransactionItems(mappedItems)

            // Reduce stock for each product
            for (item in mappedItems) {
                if (item.productId > 0) {
                    productDao.reduceStock(item.productId, item.quantity)
                }
            }
            transactionId
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        if (productDao.countProducts() == 0) {
            val sampleProducts = listOf(
                Product(
                    name = "Kopi Susu Gula Aren",
                    barcode = "8991001",
                    category = "Minuman",
                    costPrice = 8000,
                    sellingPrice = 15000,
                    stock = 35,
                    minStockAlert = 10,
                    unit = "cup"
                ),
                Product(
                    name = "Es Teh Manis Jumbo",
                    barcode = "8991002",
                    category = "Minuman",
                    costPrice = 2000,
                    sellingPrice = 5000,
                    stock = 50,
                    minStockAlert = 10,
                    unit = "cup"
                ),
                Product(
                    name = "Matcha Latte Ice",
                    barcode = "8991003",
                    category = "Minuman",
                    costPrice = 10000,
                    sellingPrice = 18000,
                    stock = 25,
                    minStockAlert = 5,
                    unit = "cup"
                ),
                Product(
                    name = "Roti Bakar Cokelat Keju",
                    barcode = "8992001",
                    category = "Makanan",
                    costPrice = 9000,
                    sellingPrice = 16000,
                    stock = 20,
                    minStockAlert = 5,
                    unit = "porsi"
                ),
                Product(
                    name = "Nasi Goreng Spesial",
                    barcode = "8992002",
                    category = "Makanan",
                    costPrice = 12000,
                    sellingPrice = 22000,
                    stock = 30,
                    minStockAlert = 5,
                    unit = "porsi"
                ),
                Product(
                    name = "Mie Instan Telur Kornet",
                    barcode = "8992003",
                    category = "Makanan",
                    costPrice = 7000,
                    sellingPrice = 13000,
                    stock = 4, // Menipis
                    minStockAlert = 5,
                    unit = "porsi"
                ),
                Product(
                    name = "Keripik Singkong Balado",
                    barcode = "8993001",
                    category = "Snack",
                    costPrice = 6000,
                    sellingPrice = 10000,
                    stock = 24,
                    minStockAlert = 6,
                    unit = "bungkus"
                ),
                Product(
                    name = "Biskuit Cokelat Sandwich",
                    barcode = "8993002",
                    category = "Snack",
                    costPrice = 5500,
                    sellingPrice = 8500,
                    stock = 3, // Menipis
                    minStockAlert = 5,
                    unit = "bungkus"
                ),
                Product(
                    name = "Beras Pandan Wangi 5kg",
                    barcode = "8994001",
                    category = "Sembako",
                    costPrice = 64000,
                    sellingPrice = 75000,
                    stock = 12,
                    minStockAlert = 3,
                    unit = "karung"
                ),
                Product(
                    name = "Minyak Goreng Refill 1L",
                    barcode = "8994002",
                    category = "Sembako",
                    costPrice = 14500,
                    sellingPrice = 17500,
                    stock = 18,
                    minStockAlert = 5,
                    unit = "pouch"
                ),
                Product(
                    name = "Gula Pasir Kristal 1kg",
                    barcode = "8994003",
                    category = "Sembako",
                    costPrice = 13500,
                    sellingPrice = 16500,
                    stock = 20,
                    minStockAlert = 5,
                    unit = "bungkus"
                ),
                Product(
                    name = "Jasa Desain & Cetak Nota A4",
                    barcode = "8995001",
                    category = "Jasa",
                    costPrice = 500,
                    sellingPrice = 2500,
                    stock = 999,
                    minStockAlert = 0,
                    unit = "lembar"
                )
            )
            productDao.insertProducts(sampleProducts)
        }

        if (storeProfileDao.getStoreProfileSync() == null) {
            storeProfileDao.insertOrUpdate(
                StoreProfile(
                    id = 1,
                    storeName = "KasirKilat Mart & Cafe",
                    phone = "0812-3456-7890",
                    address = "Jl. Merdeka No. 88, Pusat Niaga",
                    receiptHeaderNote = "Selamat Berbelanja!",
                    receiptFooterNote = "Terima kasih telah berbelanja di KasirKilat!\nBarang yang sudah dibeli tidak dapat ditukar.",
                    taxPercent = 0.0,
                    isTaxEnabled = false
                )
            )
        }
    }
}

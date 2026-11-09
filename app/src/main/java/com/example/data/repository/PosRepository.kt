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
    private val categoryDao = database.categoryDao()

    val allProducts: Flow<List<Product>> = productDao.getAllActiveProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val allManagedCategories: Flow<List<com.example.data.local.entity.CategoryEntity>> = categoryDao.getAllCategories()
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
    val storeProfile: Flow<StoreProfile?> = storeProfileDao.getStoreProfile()
    val totalRevenue: Flow<Long?> = transactionDao.getTotalRevenue()
    val totalProfit: Flow<Long?> = transactionDao.getTotalProfit()
    val totalTransactionCount: Flow<Int> = transactionDao.getTransactionCount()
    val topSellingProducts: Flow<List<TopProductRaw>> = transactionDao.getTopSellingProducts(5)

    val totalInventoryCost: Flow<Long?> = productDao.getTotalInventoryCost()
    val totalInventoryValue: Flow<Long?> = productDao.getTotalInventoryValue()
    val totalStockUnits: Flow<Int?> = productDao.getTotalStockUnits()

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

    suspend fun adjustStock(productId: Long, delta: Int) {
        productDao.adjustStock(productId, delta)
    }

    suspend fun updateStock(productId: Long, newStock: Int) {
        productDao.updateStock(productId, newStock)
    }

    suspend fun updateStoreProfile(profile: StoreProfile) {
        storeProfileDao.insertOrUpdate(profile)
    }

    suspend fun addCategory(name: String): Long {
        return categoryDao.insertCategory(com.example.data.local.entity.CategoryEntity(name = name.trim()))
    }

    suspend fun updateCategory(id: Long, oldName: String, newName: String) {
        database.withTransaction {
            categoryDao.updateCategory(com.example.data.local.entity.CategoryEntity(id = id, name = newName.trim()))
            categoryDao.renameProductCategories(oldName.trim(), newName.trim())
        }
    }

    suspend fun deleteCategory(category: com.example.data.local.entity.CategoryEntity, deleteProductsAlso: Boolean = false) {
        database.withTransaction {
            categoryDao.deleteCategory(category)
            if (deleteProductsAlso) {
                categoryDao.deleteProductsInCategory(category.name)
            } else {
                categoryDao.reassignProductsToDefault(category.name)
            }
        }
    }

    suspend fun cleanDefaultUnwantedCategoriesAndProducts() {
        database.withTransaction {
            categoryDao.deleteDefaultSampleCategories()
            categoryDao.deleteDefaultSampleProducts()
            categoryDao.deleteDefaultUnwantedCategories()
            categoryDao.deleteDefaultUnwantedProducts()
        }
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

    suspend fun getActiveProductsList(): List<Product> {
        return productDao.getAllActiveProductsList()
    }

    suspend fun restoreCatalogFromCloud(catalog: com.example.data.cloud.RestoredStoreCatalog) {
        database.withTransaction {
            // Remove previous local products to prevent duplicates or obsolete items from old phone
            productDao.deleteAllProducts()

            // Insert restored products from Cloud
            val newProducts = catalog.products.map { cp ->
                Product(
                    name = cp.name,
                    barcode = cp.barcode,
                    category = cp.category,
                    sellingPrice = cp.sellingPrice,
                    costPrice = cp.costPrice,
                    stock = cp.stock,
                    unit = cp.unit,
                    isActive = cp.isActive
                )
            }
            if (newProducts.isNotEmpty()) {
                productDao.insertProducts(newProducts)
            }

            // Insert restored categories
            for (cat in catalog.categories) {
                val catClean = cat.trim()
                if (catClean.isNotBlank() && catClean != "Semua") {
                    categoryDao.insertCategory(com.example.data.local.entity.CategoryEntity(name = catClean))
                }
            }

            // Update store profile info
            if (catalog.storeName.isNotBlank()) {
                val current = storeProfileDao.getStoreProfileSync() ?: StoreProfile()
                storeProfileDao.insertOrUpdate(
                    current.copy(
                        storeName = catalog.storeName,
                        address = if (catalog.address.isNotBlank()) catalog.address else current.address,
                        phone = if (catalog.phone.isNotBlank()) catalog.phone else current.phone
                    )
                )
            }
        }
    }

    suspend fun seedInitialDataIfNeeded() {
        // Automatically purge any dummy/sample grocery categories and products
        cleanDefaultUnwantedCategoriesAndProducts()

        if (storeProfileDao.getStoreProfileSync() == null) {
            storeProfileDao.insertOrUpdate(
                StoreProfile(
                    id = 1,
                    storeName = "KasirKilat POS & Toko",
                    phone = "0812-3456-7890",
                    address = "Pusat Niaga",
                    receiptHeaderNote = "Selamat Berbelanja!",
                    receiptFooterNote = "Terima kasih telah berbelanja di toko kami!\nBarang yang sudah dibeli tidak dapat ditukar.",
                    taxPercent = 0.0,
                    isTaxEnabled = false
                )
            )
        }
    }
}

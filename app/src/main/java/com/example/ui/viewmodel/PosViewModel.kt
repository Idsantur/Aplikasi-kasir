package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.TopProductRaw
import com.example.data.local.entity.Product
import com.example.data.local.entity.StoreProfile
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.model.TransactionWithItems
import com.example.data.repository.PosRepository
import com.example.ui.model.CartItem
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PosRepository(db)
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
        }
    }

    // Products & Categories
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        selectedCategory
    ) { products, query, category ->
        products.filter { product ->
            val matchCategory = category == "Semua" || product.category.equals(category, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.barcode.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Management
    private val _cart = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    val cart: StateFlow<Map<Long, CartItem>> = _cart.asStateFlow()

    val cartItemList: StateFlow<List<CartItem>> = _cart.combine(MutableStateFlow(Unit)) { cartMap, _ ->
        cartMap.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotalItems: StateFlow<Int> = _cart.combine(MutableStateFlow(Unit)) { cartMap, _ ->
        cartMap.values.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartSubtotal: StateFlow<Long> = _cart.combine(MutableStateFlow(Unit)) { cartMap, _ ->
        cartMap.values.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Store Profile
    val storeProfile: StateFlow<StoreProfile> = repository.storeProfile
        .combine(MutableStateFlow(Unit)) { profile, _ ->
            profile ?: StoreProfile()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreProfile())

    // Transactions & History
    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics / Reports
    val totalRevenue: StateFlow<Long> = repository.totalRevenue
        .combine(MutableStateFlow(Unit)) { rev, _ -> rev ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalProfit: StateFlow<Long> = repository.totalProfit
        .combine(MutableStateFlow(Unit)) { prof, _ -> prof ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalTransactionCount: StateFlow<Int> = repository.totalTransactionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topSellingProducts: StateFlow<List<TopProductRaw>> = repository.topSellingProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Receipt Dialog State
    private val _latestCompletedReceipt = MutableStateFlow<TransactionWithItems?>(null)
    val latestCompletedReceipt: StateFlow<TransactionWithItems?> = _latestCompletedReceipt.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addToCart(product: Product) {
        val currentCart = _cart.value.toMutableMap()
        val currentItem = currentCart[product.id]
        if (currentItem != null) {
            // Check stock limit if applicable
            if (product.stock > 0 && currentItem.quantity >= product.stock) {
                // Maximum stock reached
                return
            }
            currentCart[product.id] = currentItem.copy(quantity = currentItem.quantity + 1)
        } else {
            if (product.stock > 0 || product.category.equals("Jasa", ignoreCase = true)) {
                currentCart[product.id] = CartItem(product = product, quantity = 1)
            }
        }
        _cart.value = currentCart
    }

    fun decreaseQuantity(productId: Long) {
        val currentCart = _cart.value.toMutableMap()
        val currentItem = currentCart[productId] ?: return
        if (currentItem.quantity > 1) {
            currentCart[productId] = currentItem.copy(quantity = currentItem.quantity - 1)
        } else {
            currentCart.remove(productId)
        }
        _cart.value = currentCart
    }

    fun removeFromCart(productId: Long) {
        val currentCart = _cart.value.toMutableMap()
        currentCart.remove(productId)
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun checkout(
        paymentMethod: String,
        amountPaid: Long,
        discountAmount: Long = 0L,
        taxAmount: Long = 0L,
        customerName: String = "",
        customerNote: String = "",
        onSuccess: (TransactionWithItems) -> Unit
    ) {
        viewModelScope.launch {
            val items = _cart.value.values.toList()
            if (items.isEmpty()) return@launch

            val subtotal = items.sumOf { it.subtotal }
            val totalCost = items.sumOf { it.costTotal }
            val grandTotal = (subtotal - discountAmount + taxAmount).coerceAtLeast(0L)
            val netProfit = (grandTotal - taxAmount - totalCost)
            val change = (amountPaid - grandTotal).coerceAtLeast(0L)

            val invoice = CurrencyFormatter.generateInvoiceNumber()
            val transaction = TransactionEntity(
                invoiceNumber = invoice,
                timestamp = System.currentTimeMillis(),
                subtotal = subtotal,
                discountAmount = discountAmount,
                taxAmount = taxAmount,
                serviceAmount = 0L,
                grandTotal = grandTotal,
                totalCost = totalCost,
                netProfit = netProfit,
                paymentMethod = paymentMethod,
                amountPaid = if (paymentMethod == "TUNAI") amountPaid else grandTotal,
                changeAmount = if (paymentMethod == "TUNAI") change else 0L,
                customerName = customerName,
                customerNote = customerNote
            )

            val itemEntities = items.map {
                TransactionItemEntity(
                    transactionId = 0L, // will be set by repository
                    productId = it.product.id,
                    productName = it.product.name,
                    category = it.product.category,
                    quantity = it.quantity,
                    costPrice = it.product.costPrice,
                    sellingPrice = it.product.sellingPrice,
                    itemSubtotal = it.subtotal
                )
            }

            val transactionId = repository.checkout(transaction, itemEntities)
            val completedWithItems = TransactionWithItems(
                transaction = transaction.copy(id = transactionId),
                items = itemEntities.map { it.copy(transactionId = transactionId) }
            )

            _latestCompletedReceipt.value = completedWithItems
            clearCart()
            onSuccess(completedWithItems)
        }
    }

    fun dismissReceipt() {
        _latestCompletedReceipt.value = null
    }

    fun showReceiptDetails(transaction: TransactionWithItems) {
        _latestCompletedReceipt.value = transaction
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0L) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            // also remove from cart if present
            removeFromCart(product.id)
        }
    }

    fun saveStoreProfile(profile: StoreProfile) {
        viewModelScope.launch {
            repository.updateStoreProfile(profile)
        }
    }
}

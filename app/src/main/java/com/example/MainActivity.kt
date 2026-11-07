package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CartDetailSheet
import com.example.ui.components.PaymentBottomSheet
import com.example.ui.components.ReceiptDialog
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.StoreSettingsScreen
import com.example.ui.theme.KasirKilatTheme
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch

enum class PosTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    CASHIER("Kasir", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    PRODUCTS("Produk", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    HISTORY("Riwayat", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    REPORTS("Laporan", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    SETTINGS("Toko", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KasirKilatTheme {
                KasirApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirApp(viewModel: PosViewModel = viewModel()) {
    var currentTab by remember { mutableStateOf(PosTab.CASHIER) }
    val scope = rememberCoroutineScope()

    // Observable States
    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProducts by viewModel.allProducts.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartItemList by viewModel.cartItemList.collectAsStateWithLifecycle()
    val cartTotalItems by viewModel.cartTotalItems.collectAsStateWithLifecycle()
    val cartSubtotal by viewModel.cartSubtotal.collectAsStateWithLifecycle()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val totalProfit by viewModel.totalProfit.collectAsStateWithLifecycle()
    val totalTxCount by viewModel.totalTransactionCount.collectAsStateWithLifecycle()
    val topSelling by viewModel.topSellingProducts.collectAsStateWithLifecycle()

    val activeReceipt by viewModel.latestCompletedReceipt.collectAsStateWithLifecycle()

    // Sheet states
    var showCartSheet by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle back button: navigate back to Cashier tab first
    BackHandler(enabled = currentTab != PosTab.CASHIER) {
        currentTab = PosTab.CASHIER
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                PosTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                PosTab.CASHIER -> {
                    CashierScreen(
                        storeProfile = storeProfile,
                        products = filteredProducts,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        cart = cart,
                        cartTotalItems = cartTotalItems,
                        cartSubtotal = cartSubtotal,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategorySelect = { viewModel.setSelectedCategory(it) },
                        onAddToCart = { viewModel.addToCart(it) },
                        onOpenCart = { showCartSheet = true },
                        onOpenPayment = { showPaymentSheet = true }
                    )
                }
                PosTab.PRODUCTS -> {
                    ProductsScreen(
                        products = allProducts,
                        onSaveProduct = { viewModel.saveProduct(it) },
                        onDeleteProduct = { viewModel.deleteProduct(it) }
                    )
                }
                PosTab.HISTORY -> {
                    HistoryScreen(
                        transactions = allTransactions,
                        onSelectTransaction = { viewModel.showReceiptDetails(it) }
                    )
                }
                PosTab.REPORTS -> {
                    ReportsScreen(
                        totalRevenue = totalRevenue,
                        totalProfit = totalProfit,
                        transactionCount = totalTxCount,
                        topSelling = topSelling
                    )
                }
                PosTab.SETTINGS -> {
                    StoreSettingsScreen(
                        storeProfile = storeProfile,
                        onSaveProfile = { viewModel.saveStoreProfile(it) }
                    )
                }
            }
        }

        // Cart Detail Modal Sheet
        if (showCartSheet) {
            CartDetailSheet(
                sheetState = cartSheetState,
                cartItems = cartItemList,
                subtotal = cartSubtotal,
                onDismiss = { showCartSheet = false },
                onIncrement = { viewModel.addToCart(it.product) },
                onDecrement = { viewModel.decreaseQuantity(it.product.id) },
                onRemove = { viewModel.removeFromCart(it.product.id) },
                onClearCart = { viewModel.clearCart() },
                onProceedToPayment = {
                    showCartSheet = false
                    showPaymentSheet = true
                }
            )
        }

        // Payment Checkout Modal Sheet
        if (showPaymentSheet) {
            PaymentBottomSheet(
                sheetState = paymentSheetState,
                subtotal = cartSubtotal,
                storeProfile = storeProfile,
                onDismiss = { showPaymentSheet = false },
                onConfirmCheckout = { method, paid, discount, tax, name, note ->
                    viewModel.checkout(
                        paymentMethod = method,
                        amountPaid = paid,
                        discountAmount = discount,
                        taxAmount = tax,
                        customerName = name,
                        customerNote = note,
                        onSuccess = {
                            scope.launch {
                                paymentSheetState.hide()
                                showPaymentSheet = false
                            }
                        }
                    )
                }
            )
        }

        // Authentic Thermal Receipt Dialog
        activeReceipt?.let { receipt ->
            ReceiptDialog(
                transactionWithItems = receipt,
                storeProfile = storeProfile,
                onDismiss = { viewModel.dismissReceipt() }
            )
        }
    }
}

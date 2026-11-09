package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.Product
import com.example.ui.components.AiPhotoMenuDialog
import com.example.ui.components.CategoryManagementDialog
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.WarningRed
import com.example.util.CurrencyFormatter

enum class StockFilterStatus(val label: String) {
    ALL("Semua"),
    LOW_STOCK("Stok Menipis"),
    OUT_OF_STOCK("Stok Habis"),
    SAFE("Stok Aman")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductsScreen(
    products: List<Product>,
    managedCategories: List<CategoryEntity> = emptyList(),
    totalInventoryCost: Long = 0L,
    totalInventorySellingValue: Long = 0L,
    totalStockUnits: Int = 0,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAdjustStock: (Product, Int) -> Unit = { _, _ -> },
    onAddCategory: (String) -> Unit = {},
    onUpdateCategory: (id: Long, oldName: String, newName: String) -> Unit = { _, _, _ -> },
    onDeleteCategory: (CategoryEntity, Boolean) -> Unit = { _, _ -> },
    onCleanDefaultCategories: () -> Unit = {},
    onUpdateProductCategory: (Product, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStockFilter by remember { mutableStateOf(StockFilterStatus.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    var showFormDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAiMenuDialog by remember { mutableStateOf(false) }
    var productToChangeCategory by remember { mutableStateOf<Product?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToAdjustStock by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val defaultUnwantedCategories = remember { setOf("makanan", "minuman", "cemilan", "snak", "snack", "sembako", "jasa") }
    val cleanProducts = remember(products) {
        products.filter { !defaultUnwantedCategories.contains(it.category.lowercase().trim()) }
    }

    val allCategoryNames = remember(managedCategories, cleanProducts) {
        val managed = managedCategories.map { it.name }.filter { !defaultUnwantedCategories.contains(it.lowercase().trim()) }
        val fromProds = cleanProducts.map { it.category }.filter { !defaultUnwantedCategories.contains(it.lowercase().trim()) }
        listOf("Semua") + (managed + fromProds).distinct().filter { it != "Semua" && it.isNotBlank() }
    }

    val lowStockCount = remember(cleanProducts) {
        cleanProducts.count { it.stock > 0 && it.stock <= it.minStockAlert }
    }

    val outOfStockCount = remember(cleanProducts) {
        cleanProducts.count { it.stock <= 0 && !it.category.equals("Jasa", ignoreCase = true) }
    }

    val filteredProducts = remember(cleanProducts, searchQuery, selectedStockFilter, selectedCategoryFilter) {
        cleanProducts.filter { product ->
            val matchQuery = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true)

            val matchCategory = selectedCategoryFilter == "Semua" ||
                    product.category.equals(selectedCategoryFilter, ignoreCase = true)

            val matchStockStatus = when (selectedStockFilter) {
                StockFilterStatus.ALL -> true
                StockFilterStatus.LOW_STOCK -> product.stock > 0 && product.stock <= product.minStockAlert
                StockFilterStatus.OUT_OF_STOCK -> product.stock <= 0 && !product.category.equals("Jasa", ignoreCase = true)
                StockFilterStatus.SAFE -> product.stock > product.minStockAlert || product.category.equals("Jasa", ignoreCase = true)
            }

            matchQuery && matchCategory && matchStockStatus
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Inventory Valuation Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Manajemen Stok & Menu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Kelola katalog, ganti kategori & tambah menu via Foto AI",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Shortcut Row: AI Photo Menu & Manage Categories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAiMenuDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_ai_photo_menu_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Menu Foto AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_manage_categories_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kelola Kategori", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Inventory Asset Valuation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Aset Modal Stok",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalInventoryCost),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Fisik Unit",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$totalStockUnits Barang",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("product_search_field"),
                        placeholder = { Text("Cari produk, SKU, atau kategori...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stock Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StockFilterStatus.values().forEach { status ->
                            val isSelected = selectedStockFilter == status
                            val badgeCount = when (status) {
                                StockFilterStatus.ALL -> products.size
                                StockFilterStatus.LOW_STOCK -> lowStockCount
                                StockFilterStatus.OUT_OF_STOCK -> outOfStockCount
                                StockFilterStatus.SAFE -> products.size - lowStockCount - outOfStockCount
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStockFilter = status },
                                label = {
                                    Text(
                                        text = "${status.label} ($badgeCount)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (status) {
                                        StockFilterStatus.LOW_STOCK -> MaterialTheme.colorScheme.tertiary
                                        StockFilterStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Category Chips Bar
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(allCategoryNames) { cat ->
                            val isCatSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showCategoryDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Kelola",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada produk yang sesuai",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Coba ubah kata kunci pencarian, filter, atau tambah menu baru",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAiMenuDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Foto Menu Pakai AI")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isLow = product.stock > 0 && product.stock <= product.minStockAlert
                        val isOut = product.stock <= 0 && !product.category.equals("Jasa", ignoreCase = true)
                        val profit = product.sellingPrice - product.costPrice
                        val profitMargin = if (product.costPrice > 0) {
                            (profit.toDouble() / product.costPrice.toDouble() * 100).toInt()
                        } else {
                            100
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .testTag("product_item_${product.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Clickable category pill to quickly change category
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { productToChangeCategory = product }
                                                    .testTag("change_category_pill_${product.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = product.category,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = "Ganti Kategori",
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            if (product.barcode.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "SKU: ${product.barcode}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Action buttons: Edit & Delete
                                    Row {
                                        IconButton(
                                            onClick = {
                                                productToEdit = product
                                                showFormDialog = true
                                            },
                                            modifier = Modifier.size(32.dp).testTag("edit_product_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Produk",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { productToDelete = product },
                                            modifier = Modifier.size(32.dp).testTag("delete_product_${product.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Produk",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Jual: ${CurrencyFormatter.formatRupiah(product.sellingPrice)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MoneyGreen
                                        )
                                        Text(
                                            text = "Modal: ${CurrencyFormatter.formatRupiah(product.costPrice)} (+$profitMargin%)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Stock pill with quick adjustment trigger
                                    Surface(
                                        color = when {
                                            isOut -> Color(0xFFFFEBEE)
                                            isLow -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFE8F5E9)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { productToAdjustStock = product }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isOut -> WarningRed
                                                            isLow -> Color(0xFFF57F17)
                                                            else -> MoneyGreen
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (product.category.equals("Jasa", ignoreCase = true)) "Tersedia" else "Stok: ${product.stock} ${product.unit}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isOut -> WarningRed
                                                    isLow -> Color(0xFFF57F17)
                                                    else -> MoneyGreen
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "• Ubah",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dual Floating Action Buttons: AI Photo Menu & Add Manual
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { showAiMenuDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text("Menu Foto AI", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("ai_menu_fab")
            )

            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showFormDialog = true
                },
                modifier = Modifier.testTag("add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk Manual")
            }
        }
    }

    // Quick Stock Adjustment Dialog
    productToAdjustStock?.let { prod ->
        StockAdjustmentDialog(
            product = prod,
            onDismiss = { productToAdjustStock = null },
            onConfirmAdjustment = { newStock ->
                onAdjustStock(prod, newStock)
                productToAdjustStock = null
            }
        )
    }

    // Add / Edit Product Dialog
    if (showFormDialog) {
        ProductFormDialog(
            product = productToEdit,
            availableCategories = allCategoryNames.filter { it != "Semua" },
            onDismiss = { showFormDialog = false },
            onSave = {
                onSaveProduct(it)
                showFormDialog = false
            }
        )
    }

    // Category Management Dialog (Add, Rename, Delete, Clean Defaults)
    if (showCategoryDialog) {
        CategoryManagementDialog(
            categories = managedCategories,
            products = products,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = onAddCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory,
            onCleanDefaultCategories = onCleanDefaultCategories
        )
    }

    // AI Photo Menu Extractor Dialog
    if (showAiMenuDialog) {
        AiPhotoMenuDialog(
            availableCategories = allCategoryNames.filter { it != "Semua" },
            onDismiss = { showAiMenuDialog = false },
            onProductConfirmed = { newProduct ->
                onSaveProduct(newProduct)
            },
            onProductsConfirmed = { productList ->
                productList.forEach { onSaveProduct(it) }
            }
        )
    }

    // Quick Change Category Dialog for a Product
    productToChangeCategory?.let { prod ->
        QuickChangeCategoryDialog(
            product = prod,
            availableCategories = allCategoryNames.filter { it != "Semua" },
            onDismiss = { productToChangeCategory = null },
            onCategoryChanged = { newCategory ->
                onUpdateProductCategory(prod, newCategory)
                productToChangeCategory = null
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk dari Inventaris?") },
            text = {
                Column {
                    Text("Apakah Anda yakin ingin menghapus '${prod.name}'?")
                    if (prod.stock > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Peringatan: Masih terdapat ${prod.stock} ${prod.unit} stok fisik barang ini di toko.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickChangeCategoryDialog(
    product: Product,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onCategoryChanged: (String) -> Unit
) {
    var newCategoryText by remember { mutableStateOf("") }
    val defaultUnwanted = remember { setOf("makanan", "minuman", "cemilan", "snak", "snack", "sembako", "jasa") }
    val displayCategories = remember(availableCategories, product.category) {
        (availableCategories + listOf(product.category))
            .distinct()
            .filter { it.isNotBlank() && !defaultUnwanted.contains(it.lowercase()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Ganti Kategori Produk", fontWeight = FontWeight.Bold)
                Text(product.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Pilih kategori baru:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayCategories.forEach { cat ->
                        val isSelected = product.category.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onCategoryChanged(cat)
                            },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text("Atau ketik kategori baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_category_quick_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryText.isNotBlank()) {
                        onCategoryChanged(newCategoryText.trim())
                    }
                },
                enabled = newCategoryText.isNotBlank()
            ) {
                Text("Simpan Kategori Baru")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductFormDialog(
    product: Product?,
    availableCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "Umum") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "pcs") }
    var costPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "0") }
    var sellingPriceText by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "0") }
    var stockText by remember { mutableStateOf(product?.stock?.toString() ?: "10") }
    var minStockAlertText by remember { mutableStateOf(product?.minStockAlert?.toString() ?: "5") }

    val cost = costPriceText.toLongOrNull() ?: 0L
    val selling = sellingPriceText.toLongOrNull() ?: 0L
    val stock = stockText.toIntOrNull() ?: 0
    val minStock = minStockAlertText.toIntOrNull() ?: 5
    val profit = selling - cost
    val profitPercentage = if (cost > 0) ((profit.toDouble() / cost.toDouble()) * 100).toInt() else 100

    val unwanted = remember { setOf("makanan", "minuman", "cemilan", "snak", "snack", "sembako", "jasa") }
    val categorySuggestions = remember(availableCategories) {
        availableCategories.filter { it.isNotBlank() && !unwanted.contains(it.lowercase()) }
    }
    val unitSuggestions = listOf("pcs", "unit", "pack", "box", "lembar", "lusin", "kg", "meter")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (product == null) "Tambah Item Inventaris" else "Edit Item Inventaris",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk / Barang *") },
                    modifier = Modifier.fillMaxWidth().testTag("product_form_name"),
                    singleLine = true
                )

                // Category input
                Column {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        modifier = Modifier.fillMaxWidth().testTag("product_form_category"),
                        singleLine = true
                    )
                    if (categorySuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categorySuggestions.take(6).forEach { cat ->
                                FilterChip(
                                    selected = category.equals(cat, ignoreCase = true),
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Barcode / SKU with Auto-generate button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode / SKU") },
                        modifier = Modifier.weight(1f).testTag("product_form_barcode"),
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            barcode = "SKU-${(100000..999999).random()}"
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Acak", modifier = Modifier.size(16.dp))
                    }
                }

                // Satuan / Unit with quick chips
                Column {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan Unit") },
                        modifier = Modifier.fillMaxWidth().testTag("product_form_unit"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        unitSuggestions.take(6).forEach { u ->
                            FilterChip(
                                selected = unit.equals(u, ignoreCase = true),
                                onClick = { unit = u },
                                label = { Text(u, fontSize = 10.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Prices
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Modal (Beli)") },
                        prefix = { Text("Rp ") },
                        modifier = Modifier.weight(1f).testTag("product_form_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Jual *") },
                        prefix = { Text("Rp ") },
                        modifier = Modifier.weight(1f).testTag("product_form_price"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // Profit estimation preview
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Estimasi Untung Bersih:", fontSize = 12.sp)
                        Text(
                            text = "${CurrencyFormatter.formatRupiah(profit)} (+$profitPercentage%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) MoneyGreen else WarningRed
                        )
                    }
                }

                // Stock & Min Stock Alert
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Stok Fisik") },
                        modifier = Modifier.weight(1f).testTag("product_form_stock"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minStockAlertText,
                        onValueChange = { minStockAlertText = it.filter { c -> c.isDigit() } },
                        label = { Text("Batas Peringatan") },
                        modifier = Modifier.weight(1f).testTag("product_form_min_stock"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selling > 0) {
                        val newProduct = (product ?: Product(name = name)).copy(
                            name = name.trim(),
                            category = if (category.isBlank()) "Umum" else category.trim(),
                            barcode = barcode.trim(),
                            unit = if (unit.isBlank()) "pcs" else unit.trim(),
                            costPrice = cost,
                            sellingPrice = selling,
                            stock = stock,
                            minStockAlert = minStock
                        )
                        onSave(newProduct)
                    }
                },
                enabled = name.isNotBlank() && selling > 0,
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

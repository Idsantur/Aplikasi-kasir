package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.Product

@Composable
fun CategoryManagementDialog(
    categories: List<CategoryEntity>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (id: Long, oldName: String, newName: String) -> Unit,
    onDeleteCategory: (CategoryEntity, Boolean) -> Unit,
    onCleanDefaultCategories: () -> Unit = {}
) {
    var newCategoryText by remember { mutableStateOf("") }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCleanDefaultConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("category_management_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kelola Kategori",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hanya gunakan kategori yang Anda tambahkan",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // One-tap action to remove all sample grocery categories
                OutlinedButton(
                    onClick = { showCleanDefaultConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clean_default_categories_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bersihkan Kategori Contoh Toko Kelontong", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input to add new category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = { Text("Nama kategori baru...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (newCategoryText.isNotBlank()) {
                                onAddCategory(newCategoryText.trim())
                                newCategoryText = ""
                            }
                        },
                        enabled = newCategoryText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Categories list
                Text(
                    text = "Kategori Aktif (${categories.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val productCount = products.count { it.category.equals(cat.name, ignoreCase = true) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$productCount produk",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { categoryToEdit = cat },
                                        modifier = Modifier.size(30.dp).testTag("edit_category_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ganti Nama",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(30.dp).testTag("delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_category_dialog_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Selesai")
                }
            }
        }
    }

    // Clean Default Categories Confirmation Dialog
    if (showCleanDefaultConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanDefaultConfirm = false },
            title = { Text("Hapus Semua Kategori Contoh?") },
            text = {
                Text("Semua kategori contoh (Makanan, Minuman, Cemilan, Snack, Sembako, Jasa) beserta barang contohnya akan dihapus permanen. Inventaris toko Anda akan bersih sehingga bebas diisi dengan kategori usaha Anda sendiri.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCleanDefaultCategories()
                        showCleanDefaultConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Kategori Contoh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDefaultConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Rename Category Dialog
    categoryToEdit?.let { cat ->
        var renameText by remember { mutableStateOf(cat.name) }
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Ganti Nama Kategori") },
            text = {
                Column {
                    Text(
                        text = "Semua produk dengan kategori '${cat.name}' akan otomatis diperbarui.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Nama Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onUpdateCategory(cat.id, cat.name, renameText.trim())
                            categoryToEdit = null
                        }
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Category Confirmation Dialog
    categoryToDelete?.let { cat ->
        val count = products.count { it.category.equals(cat.name, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Hapus Kategori '${cat.name}'?") },
            text = {
                Column {
                    if (count > 0) {
                        Text("Kategori ini berisi $count produk. Anda dapat memilih untuk menghapus kategori beserta semua produk di dalamnya, atau hanya menghapus kategorinya saja.")
                    } else {
                        Text("Apakah Anda yakin ingin menghapus kategori '${cat.name}'?")
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (count > 0) {
                        Button(
                            onClick = {
                                onDeleteCategory(cat, true) // delete products too
                                categoryToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus Kategori & $count Produknya")
                        }

                        OutlinedButton(
                            onClick = {
                                onDeleteCategory(cat, false) // reassign products to Umum
                                categoryToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Hapus Kategori Saja (Pindah ke 'Umum')")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDeleteCategory(cat, false)
                                categoryToDelete = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus Kategori")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

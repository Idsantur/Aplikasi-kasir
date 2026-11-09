package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StoreProfile
import com.example.ui.components.BluetoothPrinterDialog
import com.example.util.BluetoothPrinterManager

import com.example.data.local.ActiveStoreSession
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PhoneAndroid

@Composable
fun StoreSettingsScreen(
    storeProfile: StoreProfile,
    activeSession: ActiveStoreSession? = null,
    onSaveProfile: (StoreProfile) -> Unit,
    onOpenAccountDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    var showPrinterDialog by remember { mutableStateOf(false) }

    var storeName by remember(storeProfile) { mutableStateOf(storeProfile.storeName) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.phone) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.address) }
    var receiptHeaderNote by remember(storeProfile) { mutableStateOf(storeProfile.receiptHeaderNote) }
    var receiptFooterNote by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooterNote) }
    var isTaxEnabled by remember(storeProfile) { mutableStateOf(storeProfile.isTaxEnabled) }
    var taxPercentText by remember(storeProfile) { mutableStateOf(storeProfile.taxPercent.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Pengaturan Toko",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Sesuaikan identitas toko dan cetakan struk",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        // Cloud Store Account & Multi-Device Sync Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_account_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeSession != null) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = if (activeSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (activeSession != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = if (activeSession != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (activeSession != null) "Akun Cloud Terhubung" else "Akun Cloud & Pindah HP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (activeSession != null)
                                        "${activeSession.phoneNumber} - ${activeSession.storeName}"
                                    else
                                        "Login Nomor HP & PIN agar menu anti hilang",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (activeSession != null)
                            "Data menu dan harga toko Anda disinkronkan ke Cloud. Saat pindah ke HP lain, cukup login dengan nomor ini."
                        else
                            "Hubungkan Nomor HP dan buat 6 digit PIN untuk mengamankan menu toko Anda di Cloud. Bila ganti HP, menu dapat dipulihkan kapan saja.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onOpenAccountDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_cloud_account_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeSession != null) "Kelola Akun & Sinkronkan" else "Masuk / Buat Akun Toko (No. HP & PIN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Store Profile Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_profile_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identitas Usaha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko / Usaha *") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_store_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor Telepon / WhatsApp") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_phone"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Toko") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_address"),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }
        }

        // Receipt Customization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Format Struk & Pajak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = receiptHeaderNote,
                        onValueChange = { receiptHeaderNote = it },
                        label = { Text("Pesan Header Struk") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_receipt_header"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = receiptFooterNote,
                        onValueChange = { receiptFooterNote = it },
                        label = { Text("Pesan Footer Struk") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_receipt_footer"),
                        minLines = 2,
                        maxLines = 3
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Aktifkan Pajak / PPN", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Otomatis dihitung saat transaksi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTaxEnabled,
                            onCheckedChange = { isTaxEnabled = it }
                        )
                    }

                    if (isTaxEnabled) {
                        OutlinedTextField(
                            value = taxPercentText,
                            onValueChange = { taxPercentText = it },
                            label = { Text("Persentase Pajak (%)") },
                            suffix = { Text("%") },
                            modifier = Modifier.fillMaxWidth().testTag("settings_tax_percent"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Bluetooth Thermal Printer Card
        item {
            val selectedPrinter = remember(showPrinterDialog) {
                BluetoothPrinterManager.getSelectedPrinter(context)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_printer_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Printer Bluetooth Thermal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Mendukung printer thermal 58mm / 80mm ESC/POS (Goojprt, Panda, Iware, Eppos, dll).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Printer Aktif:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedPrinter?.name ?: "Belum Ada Printer Dipilih",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedPrinter != null) {
                                    Text(
                                        text = selectedPrinter.address,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (selectedPrinter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showPrinterDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_printer_settings_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (selectedPrinter == null) "Pilih & Hubungkan Printer" else "Ganti / Tes Cetak Printer")
                    }
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val updated = storeProfile.copy(
                        storeName = if (storeName.isBlank()) "KasirKilat POS" else storeName,
                        phone = phone,
                        address = address,
                        receiptHeaderNote = receiptHeaderNote,
                        receiptFooterNote = receiptFooterNote,
                        isTaxEnabled = isTaxEnabled,
                        taxPercent = taxPercentText.toDoubleOrNull() ?: 0.0
                    )
                    onSaveProfile(updated)
                    Toast.makeText(context, "Pengaturan toko berhasil disimpan!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
            }
        }

        // App Info Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "KasirKilat POS v1.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Penyimpanan lokal Room Database 100% offline, cepat, dan aman di perangkat Anda.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPrinterDialog) {
        BluetoothPrinterDialog(
            transactionWithItems = null,
            storeProfile = storeProfile,
            isTestPrintOnly = true,
            onDismiss = { showPrinterDialog = false }
        )
    }
}

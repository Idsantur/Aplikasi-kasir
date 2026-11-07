package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.StoreProfile
import com.example.data.local.model.TransactionWithItems
import com.example.ui.theme.MoneyGreen
import com.example.util.CurrencyFormatter

@Composable
fun ReceiptDialog(
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tx = transactionWithItems.transaction
    val items = transactionWithItems.items
    var showPrinterDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sukses",
                            tint = MoneyGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Transaksi Berhasil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal receipt paper container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .background(Color(0xFFFAFCF9), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFDCE4DD), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Info
                        Text(
                            text = storeProfile.storeName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E2824),
                            textAlign = TextAlign.Center
                        )
                        if (storeProfile.address.isNotBlank()) {
                            Text(
                                text = storeProfile.address,
                                fontSize = 12.sp,
                                color = Color(0xFF55635C),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (storeProfile.phone.isNotBlank()) {
                            Text(
                                text = "Telp: ${storeProfile.phone}",
                                fontSize = 12.sp,
                                color = Color(0xFF55635C),
                                textAlign = TextAlign.Center
                            )
                        }

                        ReceiptDashedDivider()

                        // Invoice & Date
                        ReceiptRow(label = "No. Nota", value = tx.invoiceNumber)
                        ReceiptRow(label = "Waktu", value = CurrencyFormatter.formatDateTime(tx.timestamp))
                        if (tx.customerName.isNotBlank()) {
                            ReceiptRow(label = "Pelanggan", value = tx.customerName)
                        }
                        ReceiptRow(label = "Metode", value = tx.paymentMethod)

                        ReceiptDashedDivider()

                        // Itemized Table
                        items.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.productName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E2824)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity}x @${CurrencyFormatter.formatRupiah(item.sellingPrice)}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF55635C)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.itemSubtotal),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E2824)
                                    )
                                }
                            }
                        }

                        ReceiptDashedDivider()

                        // Totals
                        ReceiptRow(label = "Subtotal", value = CurrencyFormatter.formatRupiah(tx.subtotal))
                        if (tx.discountAmount > 0) {
                            ReceiptRow(
                                label = "Diskon",
                                value = "-${CurrencyFormatter.formatRupiah(tx.discountAmount)}",
                                valueColor = Color(0xFFC62828)
                            )
                        }
                        if (tx.taxAmount > 0) {
                            ReceiptRow(label = "Pajak", value = CurrencyFormatter.formatRupiah(tx.taxAmount))
                        }

                        ReceiptDashedDivider()

                        // Grand Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E2824)
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(tx.grandTotal),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MoneyGreen
                            )
                        }

                        if (tx.paymentMethod == "TUNAI") {
                            ReceiptRow(label = "Tunai Diterima", value = CurrencyFormatter.formatRupiah(tx.amountPaid))
                            ReceiptRow(
                                label = "Kembalian",
                                value = CurrencyFormatter.formatRupiah(tx.changeAmount),
                                valueWeight = FontWeight.Bold
                            )
                        }

                        ReceiptDashedDivider()

                        // Footer Note
                        Text(
                            text = storeProfile.receiptFooterNote,
                            fontSize = 11.sp,
                            color = Color(0xFF6B7A72),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action: Cetak Printer Bluetooth
                Button(
                    onClick = { showPrinterDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("print_bluetooth_receipt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Printer Bluetooth", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Actions: Kirim/Bagikan & Selesai
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareReceiptText(context, transactionWithItems, storeProfile)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim / Bagikan")
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finish_receipt_button")
                    ) {
                        Text("Selesai")
                    }
                }
            }
        }
    }

    if (showPrinterDialog) {
        BluetoothPrinterDialog(
            transactionWithItems = transactionWithItems,
            storeProfile = storeProfile,
            onDismiss = { showPrinterDialog = false }
        )
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E2824),
    valueWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF55635C)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = valueWeight,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
            fontSize = 11.sp,
            color = Color(0xFFBAC4BE),
            letterSpacing = 2.sp,
            maxLines = 1
        )
    }
}

private fun shareReceiptText(
    context: Context,
    transactionWithItems: TransactionWithItems,
    storeProfile: StoreProfile
) {
    val tx = transactionWithItems.transaction
    val items = transactionWithItems.items

    val sb = StringBuilder()
    sb.appendLine("===============================")
    sb.appendLine(storeProfile.storeName.uppercase())
    if (storeProfile.address.isNotBlank()) sb.appendLine(storeProfile.address)
    if (storeProfile.phone.isNotBlank()) sb.appendLine("Telp: ${storeProfile.phone}")
    sb.appendLine("===============================")
    sb.appendLine("No. Nota : ${tx.invoiceNumber}")
    sb.appendLine("Tanggal  : ${CurrencyFormatter.formatDateTime(tx.timestamp)}")
    if (tx.customerName.isNotBlank()) sb.appendLine("Pelanggan: ${tx.customerName}")
    sb.appendLine("Metode   : ${tx.paymentMethod}")
    sb.appendLine("-------------------------------")
    for (item in items) {
        sb.appendLine(item.productName)
        sb.appendLine("  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellingPrice)} = ${CurrencyFormatter.formatRupiah(item.itemSubtotal)}")
    }
    sb.appendLine("-------------------------------")
    sb.appendLine("Subtotal : ${CurrencyFormatter.formatRupiah(tx.subtotal)}")
    if (tx.discountAmount > 0) {
        sb.appendLine("Diskon   : -${CurrencyFormatter.formatRupiah(tx.discountAmount)}")
    }
    if (tx.taxAmount > 0) {
        sb.appendLine("Pajak    : ${CurrencyFormatter.formatRupiah(tx.taxAmount)}")
    }
    sb.appendLine("TOTAL    : ${CurrencyFormatter.formatRupiah(tx.grandTotal)}")
    if (tx.paymentMethod == "TUNAI") {
        sb.appendLine("Bayar    : ${CurrencyFormatter.formatRupiah(tx.amountPaid)}")
        sb.appendLine("Kembali  : ${CurrencyFormatter.formatRupiah(tx.changeAmount)}")
    }
    sb.appendLine("===============================")
    sb.appendLine(storeProfile.receiptFooterNote)

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Struk Belanja"))
}

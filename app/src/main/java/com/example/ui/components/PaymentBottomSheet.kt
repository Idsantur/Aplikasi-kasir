package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StoreProfile
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.WarningRed
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentBottomSheet(
    sheetState: SheetState,
    subtotal: Long,
    storeProfile: StoreProfile,
    onDismiss: () -> Unit,
    onConfirmCheckout: (
        paymentMethod: String,
        amountPaid: Long,
        discountAmount: Long,
        taxAmount: Long,
        customerName: String,
        customerNote: String
    ) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("TUNAI") }
    var discountText by remember { mutableStateOf("") }
    var applyTax by remember { mutableStateOf(storeProfile.isTaxEnabled) }
    var cashPaidText by remember { mutableStateOf(subtotal.toString()) }
    var customerName by remember { mutableStateOf("") }
    var customerNote by remember { mutableStateOf("") }

    val discountAmount = discountText.toLongOrNull() ?: 0L
    val taxAmount = if (applyTax && storeProfile.taxPercent > 0) {
        ((subtotal - discountAmount) * (storeProfile.taxPercent / 100.0)).toLong().coerceAtLeast(0L)
    } else {
        0L
    }
    val grandTotal = (subtotal - discountAmount + taxAmount).coerceAtLeast(0L)
    val cashPaid = cashPaidText.toLongOrNull() ?: 0L
    val changeAmount = cashPaid - grandTotal
    val isCashSufficient = selectedMethod != "TUNAI" || changeAmount >= 0

    // Preset Cash amounts
    val presetAmounts = remember(grandTotal) {
        val list = mutableListOf<Long>()
        list.add(grandTotal) // Uang pas
        val standardDenominations = listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L)
        for (denom in standardDenominations) {
            if (denom > grandTotal && !list.contains(denom)) {
                list.add(denom)
            }
        }
        // Also round up to nearest 10k/50k
        val roundedTenK = ((grandTotal + 9999) / 10000) * 10000
        if (roundedTenK > grandTotal && !list.contains(roundedTenK)) {
            list.add(roundedTenK)
        }
        val roundedFiftyK = ((grandTotal + 49999) / 50000) * 50000
        if (roundedFiftyK > grandTotal && !list.contains(roundedFiftyK)) {
            list.add(roundedFiftyK)
        }
        list.distinct().sorted()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Pembayaran",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pilih metode dan selesaikan transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grand Total Highlight Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL TAGIHAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(grandTotal),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Methods Selector
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("TUNAI", "Tunai", Icons.Default.AttachMoney),
                    Triple("QRIS", "QRIS", Icons.Default.QrCode2),
                    Triple("DEBIT", "Debit/Transfer", Icons.Default.CreditCard)
                ).forEach { (id, label, icon) ->
                    val isSelected = selectedMethod == id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedMethod = id
                                if (id != "TUNAI") {
                                    cashPaidText = grandTotal.toString()
                                }
                            }
                            .testTag("method_${id.lowercase()}"),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cash Specific Controls
            AnimatedVisibility(visible = selectedMethod == "TUNAI") {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Uang Tunai Diterima",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = cashPaidText,
                        onValueChange = { cashPaidText = it.filter { char -> char.isDigit() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_input_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("Rp ") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetAmounts.take(5).forEach { amount ->
                            val label = if (amount == grandTotal) "Uang Pas" else CurrencyFormatter.formatRupiah(amount)
                            FilterChip(
                                selected = cashPaid == amount,
                                onClick = { cashPaidText = amount.toString() },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Change calculation card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCashSufficient) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCashSufficient) "Kembalian:" else "Uang Kurang:",
                                fontWeight = FontWeight.Bold,
                                color = if (isCashSufficient) MoneyGreen else WarningRed
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(kotlin.math.abs(changeAmount)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCashSufficient) MoneyGreen else WarningRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra Options: Diskon & Pajak
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it.filter { char -> char.isDigit() } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discount_input_field"),
                    label = { Text("Potongan / Diskon") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("customer_name_field"),
                    label = { Text("Nama Pelanggan") },
                    singleLine = true
                )
            }

            if (storeProfile.taxPercent > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Terapkan Pajak (${storeProfile.taxPercent}%)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = applyTax,
                        onCheckedChange = { applyTax = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Button
            Button(
                onClick = {
                    if (isCashSufficient) {
                        onConfirmCheckout(
                            selectedMethod,
                            if (selectedMethod == "TUNAI") cashPaid else grandTotal,
                            discountAmount,
                            taxAmount,
                            customerName,
                            customerNote
                        )
                    }
                },
                enabled = isCashSufficient && grandTotal > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_payment_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Konfirmasi & Buat Struk",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

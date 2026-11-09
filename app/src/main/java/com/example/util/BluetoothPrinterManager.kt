package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.local.entity.StoreProfile
import com.example.data.local.model.TransactionWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String
)

object BluetoothPrinterManager {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val PREF_NAME = "kasirkilat_printer_pref"
    private const val KEY_SELECTED_PRINTER_MAC = "selected_printer_mac"
    private const val KEY_SELECTED_PRINTER_NAME = "selected_printer_name"

    // Standard 58mm printer char width (32 chars)
    private const val LINE_WIDTH = 32

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothPrinterDevice> {
        if (!hasBluetoothPermission(context)) return emptyList()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()

        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices.map { device ->
                BluetoothPrinterDevice(
                    name = device.name ?: "Unknown Printer",
                    address = device.address
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSelectedPrinter(context: Context, device: BluetoothPrinterDevice) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SELECTED_PRINTER_MAC, device.address)
            .putString(KEY_SELECTED_PRINTER_NAME, device.name)
            .apply()
    }

    fun getSelectedPrinter(context: Context): BluetoothPrinterDevice? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val mac = prefs.getString(KEY_SELECTED_PRINTER_MAC, null) ?: return null
        val name = prefs.getString(KEY_SELECTED_PRINTER_NAME, "Printer Bluetooth") ?: "Printer Bluetooth"
        return BluetoothPrinterDevice(name, mac)
    }

    @SuppressLint("MissingPermission")
    suspend fun sendDataToPrinter(
        context: Context,
        deviceAddress: String,
        data: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission(context)) {
            return@withContext Result.failure(SecurityException("Izin Bluetooth belum diberikan"))
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth tidak tersedia di perangkat ini"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth sedang nonaktif. Silakan aktifkan Bluetooth."))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = adapter.getRemoteDevice(deviceAddress)

            // Direct connection to paired printer via RFCOMM socket (no discovery needed)

            // Attempt 1: Standard Serial Port Profile (SPP) UUID
            var connected = false
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                connected = true
            } catch (sppError: Exception) {
                try {
                    socket?.close()
                } catch (ignored: Exception) {}
                socket = null
            }

            // Attempt 2: Fallback RFCOMM Channel 1 via reflection (widely used by Bellav, Goojprt, Panda thermal printers)
            if (!connected) {
                try {
                    val createRfcommMethod = device.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                    socket = createRfcommMethod.invoke(device, 1) as BluetoothSocket
                    socket.connect()
                    connected = true
                } catch (fallbackError: Exception) {
                    val errMsg = fallbackError.localizedMessage ?: sppErrorOrGeneric(device.name)
                    throw Exception("Gagal terhubung ke printer '${device.name ?: deviceAddress}'. Pastikan printer menyala, baterai terisi, dan tidak terhubung ke HP lain.")
                }
            }

            val currentSocket = socket ?: throw Exception("Socket printer tidak dapat dibuat.")
            outputStream = currentSocket.outputStream
            outputStream.write(data)
            outputStream.flush()

            // Small delay to ensure all buffers flush to thermal head
            kotlinx.coroutines.delay(400)

            Result.success("Struk berhasil dicetak ke ${device.name ?: deviceAddress}")
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Gagal mencetak struk"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore close exceptions
            }
        }
    }

    private fun sppErrorOrGeneric(deviceName: String?): String {
        return "Gagal menyambung ke $deviceName"
    }

    // Generate ESC/POS Formatted Receipt Bytes
    fun buildEscPosReceipt(
        transactionWithItems: TransactionWithItems,
        storeProfile: StoreProfile
    ): ByteArray {
        val tx = transactionWithItems.transaction
        val items = transactionWithItems.items
        val out = ByteArrayOutputStream()
        val charset = Charset.forName("CP437")

        // 1. Initialize printer
        out.write(byteArrayOf(0x1B, 0x40))

        // 2. Center Align & Double Size for Store Name
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        out.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double size
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on
        out.write("${storeProfile.storeName}\n".toByteArray(charset))

        // Normal size
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off

        if (storeProfile.address.isNotBlank()) {
            out.write("${storeProfile.address}\n".toByteArray(charset))
        }
        if (storeProfile.phone.isNotBlank()) {
            out.write("Telp: ${storeProfile.phone}\n".toByteArray(charset))
        }

        out.write("${"-".repeat(LINE_WIDTH)}\n".toByteArray(charset))

        // 3. Left Align for Meta info
        out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
        out.write(formatTwoColumns("No. Nota", tx.invoiceNumber).toByteArray(charset))
        out.write(formatTwoColumns("Waktu", CurrencyFormatter.formatDateTime(tx.timestamp)).toByteArray(charset))
        if (tx.customerName.isNotBlank()) {
            out.write(formatTwoColumns("Pelanggan", tx.customerName).toByteArray(charset))
        }
        out.write(formatTwoColumns("Metode", tx.paymentMethod).toByteArray(charset))

        out.write("${"-".repeat(LINE_WIDTH)}\n".toByteArray(charset))

        // 4. Items Table
        for (item in items) {
            out.write("${item.productName}\n".toByteArray(charset))
            val leftCol = "  ${item.quantity} x ${CurrencyFormatter.formatRupiah(item.sellingPrice)}"
            val rightCol = CurrencyFormatter.formatRupiah(item.itemSubtotal)
            out.write(formatTwoColumns(leftCol, rightCol).toByteArray(charset))
        }

        out.write("${"-".repeat(LINE_WIDTH)}\n".toByteArray(charset))

        // 5. Totals
        out.write(formatTwoColumns("Subtotal", CurrencyFormatter.formatRupiah(tx.subtotal)).toByteArray(charset))
        if (tx.discountAmount > 0) {
            out.write(formatTwoColumns("Diskon", "-${CurrencyFormatter.formatRupiah(tx.discountAmount)}").toByteArray(charset))
        }
        if (tx.taxAmount > 0) {
            out.write(formatTwoColumns("Pajak", CurrencyFormatter.formatRupiah(tx.taxAmount)).toByteArray(charset))
        }

        out.write("${"=".repeat(LINE_WIDTH)}\n".toByteArray(charset))

        // Grand Total (Bold)
        out.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold on
        out.write(formatTwoColumns("TOTAL", CurrencyFormatter.formatRupiah(tx.grandTotal)).toByteArray(charset))
        out.write(byteArrayOf(0x1B, 0x45, 0x00)) // Bold off

        if (tx.paymentMethod == "TUNAI") {
            out.write(formatTwoColumns("Bayar Tunai", CurrencyFormatter.formatRupiah(tx.amountPaid)).toByteArray(charset))
            out.write(formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(tx.changeAmount)).toByteArray(charset))
        }

        out.write("${"-".repeat(LINE_WIDTH)}\n".toByteArray(charset))

        // 6. Footer Notes
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        out.write("${storeProfile.receiptFooterNote}\n".toByteArray(charset))

        // Extra lines for paper feed
        out.write("\n\n\n".toByteArray(charset))

        return out.toByteArray()
    }

    // Build Test Receipt
    fun buildTestReceipt(storeName: String): ByteArray {
        val out = ByteArrayOutputStream()
        val charset = Charset.forName("CP437")

        out.write(byteArrayOf(0x1B, 0x40)) // Init
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        out.write(byteArrayOf(0x1D, 0x21, 0x11)) // Double size
        out.write("TEST PRINTER OK\n".toByteArray(charset))
        out.write(byteArrayOf(0x1D, 0x21, 0x00)) // Normal
        out.write("$storeName\n".toByteArray(charset))
        out.write("${"=".repeat(LINE_WIDTH)}\n".toByteArray(charset))
        out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
        out.write("Printer Bluetooth Terhubung!\n".toByteArray(charset))
        out.write("Ukuran Kertas: 58mm / 32 Kolom\n".toByteArray(charset))
        out.write("Koneksi ESC/POS: Stabil & Siap\n".toByteArray(charset))
        out.write("${"-".repeat(LINE_WIDTH)}\n".toByteArray(charset))
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        out.write("KasirKilat POS Android\n\n\n".toByteArray(charset))

        return out.toByteArray()
    }

    private fun formatTwoColumns(left: String, right: String): String {
        val maxLeft = LINE_WIDTH - right.length - 1
        val truncatedLeft = if (left.length > maxLeft && maxLeft > 0) left.take(maxLeft) else left
        val spacesCount = (LINE_WIDTH - truncatedLeft.length - right.length).coerceAtLeast(1)
        return truncatedLeft + " ".repeat(spacesCount) + right + "\n"
    }
}

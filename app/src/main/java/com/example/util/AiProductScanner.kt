package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiProductScanner"

data class ScannedProductResult(
    val name: String,
    val category: String,
    val sellingPrice: Long,
    val costPrice: Long,
    val unit: String,
    val description: String = ""
)

object AiProductScanner {

    private val menuItemSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "productName" to Schema.string(description = "Nama produk, barang, atau menu yang tertera"),
                "category" to Schema.string(description = "Kategori yang paling cocok sesuai jenis barang toko (contoh: Pakaian, Aksesoris, Elektronik, Sparepart, Layanan, atau Umum)"),
                "suggestedSellingPrice" to Schema.integer(description = "Harga jual dalam angka Rupiah utuh. Contoh: 10k -> 10000, 12k -> 12000, 8k -> 8000, 13k -> 13000"),
                "suggestedCostPrice" to Schema.integer(description = "Estimasi harga modal dalam Rupiah (sekitar 50-70% harga jual)"),
                "unit" to Schema.string(description = "Satuan produk: pcs, unit, pack, box, lembar, lusin, kg, dll")
            )
        )
    }

    private val menuListSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "items" to Schema.array(
                    items = menuItemSchema,
                    description = "Daftar SEMUA item, produk, barang, atau menu yang tertera pada foto"
                )
            )
        )
    }

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = menuListSchema
            }
        )
    }

    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = menuListSchema
            }
        )
    }

    suspend fun analyzeProductPhoto(bitmap: Bitmap): Result<List<ScannedProductResult>> = withContext(Dispatchers.IO) {
        try {
            // Resize bitmap to reasonable dimension (max 1024) to reduce latency and memory overhead
            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val promptText = """
                Analisis foto ini secara mendalam. Foto bisa berupa daftar/buku/papan katalog barang atau menu dengan banyak item, atau satu produk barang satuan dari segala jenis usaha.

                INSTRUKSI PENTING:
                1. Jika foto berisi DAFTAR BARANG / KATALOG / MENU (banyak item), ekstrak SEMUA item yang ada di foto satu per satu ke dalam list 'items'. Jangan hanya ambil 1 item!
                2. Pahami format penulisan harga Indonesia:
                   - '10k' atau '10rb' = 10000
                   - '12k' = 12000
                   - '8k' = 8000
                   - '6k' = 6000
                   - '13k' = 13000
                   - '7k' = 7000
                   - '15.000' atau '15000' = 15000
                3. Tentukan kategori yang akurat berdasarkan jenis usaha atau kelompok yang tertera di foto (contoh: Pakaian, Celana, Aksesoris, Jasa, Sparepart, Elektronik, atau sesuai judul kelompok di foto). Jika tidak ada kelompok khusus, gunakan kategori yang relevan atau 'Umum'.
                4. Jika foto berupa 1 foto produk/barang saja, masukkan 1 item tersebut ke dalam list 'items'.
                5. Estimasi harga modal jika tidak tertera adalah sekitar 60-70% dari harga jual.
            """.trimIndent()

            val promptContent = content {
                image(scaledBitmap)
                text(promptText)
            }

            // Step 1: Try primary model (gemini-3.8-flash)
            var jsonString: String? = null
            var lastError: Throwable? = null

            try {
                Log.d(TAG, "Trying primary model gemini-3.8-flash...")
                val response = withTimeout(45_000L) {
                    primaryModel.generateContent(promptContent)
                }
                jsonString = response.text
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Primary model failed (${e.message}). Switching to fallback model gemini-3.5-flash-lite...")
            }

            // Step 2: Fallback to gemini-3.5-flash-lite if primary had an issue (e.g. 503 high demand or timeout)
            if (jsonString.isNullOrBlank()) {
                delay(500)
                try {
                    Log.d(TAG, "Trying fallback model gemini-3.5-flash-lite...")
                    val response = withTimeout(45_000L) {
                        fallbackModel.generateContent(promptContent)
                    }
                    jsonString = response.text
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "Fallback attempt 1 failed (${e.message}). Retrying fallback...")
                }
            }

            // Step 3: Second retry with fallback if needed
            if (jsonString.isNullOrBlank()) {
                delay(1200)
                try {
                    val response = withTimeout(45_000L) {
                        fallbackModel.generateContent(promptContent)
                    }
                    jsonString = response.text
                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "Fallback retry failed", e)
                }
            }

            if (jsonString.isNullOrBlank()) {
                val userFriendlyMessage = when {
                    lastError?.message?.contains("high demand", ignoreCase = true) == true ||
                            lastError?.message?.contains("503", ignoreCase = true) == true ->
                        "Server AI sedang padat. Silakan coba kembali dalam beberapa detik."
                    lastError?.message?.contains("timeout", ignoreCase = true) == true ->
                        "Waktu koneksi habis. Pastikan internet lancar lalu coba lagi."
                    else ->
                        lastError?.localizedMessage ?: "Gagal memproses foto dengan AI. Silakan coba lagi."
                }
                return@withContext Result.failure(Exception(userFriendlyMessage))
            }

            Log.d(TAG, "Raw AI JSON output: $jsonString")
            val root = JSONObject(jsonString)
            val itemsArray = root.optJSONArray("items")

            val results = mutableListOf<ScannedProductResult>()
            if (itemsArray != null && itemsArray.length() > 0) {
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val name = item.optString("productName", "").trim()
                    if (name.isBlank()) continue

                    val category = item.optString("category", "Umum").trim()
                    val selling = item.optLong("suggestedSellingPrice", 10000L).coerceAtLeast(1000L)
                    val cost = item.optLong("suggestedCostPrice", (selling * 0.65).toLong()).coerceAtLeast(500L)
                    val unit = item.optString("unit", "pcs").trim()

                    results.add(
                        ScannedProductResult(
                            name = name,
                            category = category.ifBlank { "Umum" },
                            sellingPrice = selling,
                            costPrice = cost,
                            unit = unit.ifBlank { "pcs" },
                            description = "Terdeteksi dari foto produk"
                        )
                    )
                }
            }

            if (results.isEmpty()) {
                // If items array was missing, try parsing single object fallback
                val name = root.optString("productName", "").trim()
                if (name.isNotBlank()) {
                    val category = root.optString("category", "Umum").trim()
                    val selling = root.optLong("suggestedSellingPrice", 10000L).coerceAtLeast(1000L)
                    val cost = root.optLong("suggestedCostPrice", (selling * 0.65).toLong())
                    val unit = root.optString("unit", "pcs").trim()
                    results.add(
                        ScannedProductResult(
                            name = name,
                            category = category.ifBlank { "Umum" },
                            sellingPrice = selling,
                            costPrice = cost,
                            unit = unit.ifBlank { "pcs" },
                            description = "Terdeteksi dari foto produk"
                        )
                    )
                }
            }

            if (results.isEmpty()) {
                Result.failure(Exception("Tidak ada teks menu yang dapat terbaca pada foto. Coba ambil foto dengan jarak lebih dekat atau pencahayaan lebih terang."))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal failure in analyzeProductPhoto", e)
            val friendlyMsg = if (e.message?.contains("high demand", ignoreCase = true) == true) {
                "Server AI sedang padat. Silakan coba kembali dalam beberapa detik."
            } else {
                e.localizedMessage ?: "Terjadi kesalahan saat memproses gambar."
            }
            Result.failure(Exception(friendlyMsg))
        }
    }
}

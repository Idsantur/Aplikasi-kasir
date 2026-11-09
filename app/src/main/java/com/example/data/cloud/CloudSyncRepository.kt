package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.entity.Product
import com.example.data.local.entity.StoreProfile
import com.example.util.OperationType
import com.example.util.PinSecurityUtil
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "CloudSyncRepository"

class CloudSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Harap hubungkan akun Google terlebih dahulu untuk sinkronisasi cloud.")
    }

    /**
     * Register a new store account keyed by phone number and protected by PIN.
     */
    suspend fun registerStore(
        phoneNumber: String,
        pin: String,
        storeName: String,
        address: String = "",
        phone: String = ""
    ): Result<CloudStoreAccount> = withContext(Dispatchers.IO) {
        val cleanPhone = PinSecurityUtil.normalizePhoneNumber(phoneNumber)
        if (!PinSecurityUtil.isValidPhoneNumber(cleanPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Nomor HP tidak valid. Contoh: 081234567890"))
        }
        if (!PinSecurityUtil.isValidPin(pin)) {
            return@withContext Result.failure(IllegalArgumentException("PIN harus berupa 6 digit angka."))
        }

        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        val docRef = db.collection("stores").document(cleanPhone)
        try {
            val existingDoc = docRef.get().await()
            if (existingDoc.exists()) {
                return@withContext Result.failure(IllegalStateException("Nomor HP $cleanPhone sudah terdaftar. Silakan pilih menu 'Masuk Akun'."))
            }

            val pinHash = PinSecurityUtil.hashPin(pin)
            val storeData = mapOf(
                "phoneNumber" to cleanPhone,
                "pinHash" to pinHash,
                "storeName" to storeName.ifBlank { "Toko Kasir" },
                "ownerUid" to uid,
                "address" to address,
                "phone" to phone.ifBlank { cleanPhone },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(storeData).await()
            Result.success(
                CloudStoreAccount(
                    id = cleanPhone,
                    phoneNumber = cleanPhone,
                    pinHash = pinHash,
                    storeName = storeName,
                    ownerUid = uid,
                    address = address,
                    phone = phone
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(Exception("Gagal mendaftarkan akun toko: ${e.localizedMessage ?: "Terjadi kesalahan pada server"}"))
        }
    }

    /**
     * Authenticate and verify an existing store account with phone number and PIN.
     */
    suspend fun loginStore(
        phoneNumber: String,
        pin: String
    ): Result<CloudStoreAccount> = withContext(Dispatchers.IO) {
        val cleanPhone = PinSecurityUtil.normalizePhoneNumber(phoneNumber)
        if (!PinSecurityUtil.isValidPhoneNumber(cleanPhone)) {
            return@withContext Result.failure(IllegalArgumentException("Nomor HP tidak valid. Masukkan nomor berawalan 08..."))
        }
        if (!PinSecurityUtil.isValidPin(pin)) {
            return@withContext Result.failure(IllegalArgumentException("PIN harus berupa 6 digit angka."))
        }

        val docRef = db.collection("stores").document(cleanPhone)
        try {
            val doc = docRef.get().await()
            if (!doc.exists()) {
                return@withContext Result.failure(IllegalStateException("Nomor HP $cleanPhone belum terdaftar. Silakan lakukan 'Daftar Akun Baru'."))
            }

            val storedPinHash = doc.getString("pinHash") ?: ""
            val inputPinHash = PinSecurityUtil.hashPin(pin)

            if (storedPinHash != inputPinHash) {
                return@withContext Result.failure(IllegalStateException("PIN yang Anda masukkan salah. Silakan coba kembali."))
            }

            val store = doc.toObject(CloudStoreAccount::class.java)
                ?: CloudStoreAccount(
                    id = cleanPhone,
                    phoneNumber = cleanPhone,
                    storeName = doc.getString("storeName") ?: "Toko Kasir",
                    ownerUid = doc.getString("ownerUid") ?: "",
                    address = doc.getString("address") ?: "",
                    phone = doc.getString("phone") ?: cleanPhone
                )

            Result.success(store)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(Exception("Gagal masuk akun: ${e.localizedMessage ?: "Periksa koneksi internet Anda."}"))
        }
    }

    /**
     * Upload local catalog (products, categories, store profile) to Cloud.
     */
    suspend fun uploadStoreCatalog(
        phoneNumber: String,
        pin: String,
        products: List<Product>,
        categories: List<String>,
        storeProfile: StoreProfile
    ): Result<Int> = withContext(Dispatchers.IO) {
        val cleanPhone = PinSecurityUtil.normalizePhoneNumber(phoneNumber)
        val loginCheck = loginStore(cleanPhone, pin)
        if (loginCheck.isFailure) {
            return@withContext Result.failure(loginCheck.exceptionOrNull() ?: Exception("Verifikasi akun gagal"))
        }

        val storeRef = db.collection("stores").document(cleanPhone)
        try {
            // 1. Update store metadata
            val storeMeta = mapOf(
                "storeName" to storeProfile.storeName,
                "address" to storeProfile.address,
                "phone" to storeProfile.phone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            storeRef.set(storeMeta, SetOptions.merge()).await()

            // 2. Upload Categories
            val categoriesColl = storeRef.collection("categories")
            for (cat in categories) {
                val catClean = cat.trim()
                if (catClean.isNotBlank() && catClean != "Semua") {
                    val catDocId = catClean.lowercase().replace(Regex("[^a-z0-9_]"), "_")
                    val catPayload = mapOf(
                        "phoneNumber" to cleanPhone,
                        "name" to catClean,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    categoriesColl.document(catDocId).set(catPayload).await()
                }
            }

            // 3. Upload Products
            val productsColl = storeRef.collection("products")
            var uploadedCount = 0
            for (prod in products) {
                val prodDocId = "prod_${prod.id}"
                val prodPayload = mapOf(
                    "phoneNumber" to cleanPhone,
                    "name" to prod.name,
                    "barcode" to prod.barcode,
                    "category" to prod.category.ifBlank { "Umum" },
                    "sellingPrice" to prod.sellingPrice,
                    "costPrice" to prod.costPrice,
                    "stock" to prod.stock,
                    "unit" to prod.unit.ifBlank { "pcs" },
                    "isActive" to prod.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                productsColl.document(prodDocId).set(prodPayload).await()
                uploadedCount++
            }

            Result.success(uploadedCount)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, storeRef.path)
            Result.failure(Exception("Gagal sinkronisasi data ke cloud: ${e.localizedMessage}"))
        }
    }

    /**
     * Download entire store catalog from Cloud when moving to a new phone.
     */
    suspend fun downloadStoreCatalog(
        phoneNumber: String,
        pin: String
    ): Result<RestoredStoreCatalog> = withContext(Dispatchers.IO) {
        val cleanPhone = PinSecurityUtil.normalizePhoneNumber(phoneNumber)
        val loginResult = loginStore(cleanPhone, pin)
        if (loginResult.isFailure) {
            return@withContext Result.failure(loginResult.exceptionOrNull() ?: Exception("Verifikasi akun gagal"))
        }

        val storeAccount = loginResult.getOrThrow()
        val storeRef = db.collection("stores").document(cleanPhone)

        try {
            // Fetch categories
            val catSnapshots = storeRef.collection("categories").get().await()
            val restoredCategories = catSnapshots.documents.mapNotNull { it.getString("name") }

            // Fetch products
            val prodSnapshots = storeRef.collection("products").get().await()
            val restoredProducts = prodSnapshots.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val barcode = doc.getString("barcode") ?: ""
                val category = doc.getString("category") ?: "Umum"
                val sellingPrice = doc.getLong("sellingPrice") ?: 0L
                val costPrice = doc.getLong("costPrice") ?: 0L
                val stock = (doc.getLong("stock") ?: 0L).toInt()
                val unit = doc.getString("unit") ?: "pcs"
                val isActive = doc.getBoolean("isActive") ?: true

                CloudProductItem(
                    id = doc.id,
                    phoneNumber = cleanPhone,
                    name = name,
                    barcode = barcode,
                    category = category,
                    sellingPrice = sellingPrice,
                    costPrice = costPrice,
                    stock = stock,
                    unit = unit,
                    isActive = isActive
                )
            }

            Result.success(
                RestoredStoreCatalog(
                    storeName = storeAccount.storeName,
                    address = storeAccount.address,
                    phone = storeAccount.phone,
                    products = restoredProducts,
                    categories = restoredCategories
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, storeRef.path)
            Result.failure(Exception("Gagal mengunduh katalog dari cloud: ${e.localizedMessage}"))
        }
    }
}

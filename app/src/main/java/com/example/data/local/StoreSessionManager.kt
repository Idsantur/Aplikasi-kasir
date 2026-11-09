package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

data class ActiveStoreSession(
    val phoneNumber: String,
    val storeName: String,
    val lastSyncTimestamp: Long
)

class StoreSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getActiveSession(): ActiveStoreSession? {
        val phone = prefs.getString(KEY_PHONE, null) ?: return null
        val storeName = prefs.getString(KEY_STORE_NAME, "Toko KasirKilat") ?: "Toko KasirKilat"
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        return ActiveStoreSession(
            phoneNumber = phone,
            storeName = storeName,
            lastSyncTimestamp = lastSync
        )
    }

    fun saveActiveSession(phoneNumber: String, storeName: String) {
        prefs.edit()
            .putString(KEY_PHONE, phoneNumber)
            .putString(KEY_STORE_NAME, storeName)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun updateLastSyncTime() {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_PHONE)
            .remove(KEY_STORE_NAME)
            .remove(KEY_LAST_SYNC)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "kasirkilat_store_session"
        private const val KEY_PHONE = "active_phone_number"
        private const val KEY_STORE_NAME = "active_store_name"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
    }
}

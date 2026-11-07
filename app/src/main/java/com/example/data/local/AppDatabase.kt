package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.StoreProfileDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.Product
import com.example.data.local.entity.StoreProfile
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity

@Database(
    entities = [
        Product::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StoreProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun storeProfileDao(): StoreProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasirkilat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

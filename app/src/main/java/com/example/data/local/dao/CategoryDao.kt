package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE products SET category = :newName WHERE category = :oldName")
    suspend fun renameProductCategories(oldName: String, newName: String)

    @Query("UPDATE products SET category = 'Umum' WHERE category = :deletedCategory")
    suspend fun reassignProductsToDefault(deletedCategory: String)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("DELETE FROM products WHERE category = :deletedCategory")
    suspend fun deleteProductsInCategory(deletedCategory: String)

    @Query("DELETE FROM categories WHERE LOWER(name) IN ('makanan', 'minuman', 'cemilan', 'snak', 'snack', 'sembako', 'jasa')")
    suspend fun deleteDefaultSampleCategories()

    @Query("DELETE FROM products WHERE LOWER(category) IN ('makanan', 'minuman', 'cemilan', 'snak', 'snack', 'sembako', 'jasa')")
    suspend fun deleteDefaultSampleProducts()

    @Query("DELETE FROM categories WHERE LOWER(name) IN ('sembako', 'jasa', 'makanan', 'minuman', 'cemilan', 'snack', 'snak')")
    suspend fun deleteDefaultUnwantedCategories()

    @Query("DELETE FROM products WHERE LOWER(category) IN ('sembako', 'jasa', 'makanan', 'minuman', 'cemilan', 'snack', 'snak')")
    suspend fun deleteDefaultUnwantedProducts()
}

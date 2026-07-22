package com.wwwescape.pixelebookreader.data.category

import android.content.Context
import com.wwwescape.pixelebookreader.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow

object CategoryRepository {
    fun getAllCategories(context: Context): Flow<List<Category>> =
        AppDatabase.getInstance(context).categoryDao().getAll()

    suspend fun addCategory(context: Context, category: Category): Long =
        AppDatabase.getInstance(context).categoryDao().insert(category)

    suspend fun updateCategory(context: Context, category: Category) =
        AppDatabase.getInstance(context).categoryDao().update(category)

    suspend fun deleteCategory(context: Context, category: Category) =
        AppDatabase.getInstance(context).categoryDao().delete(category)

    suspend fun deleteAllCategories(context: Context) =
        AppDatabase.getInstance(context).categoryDao().deleteAll()
}

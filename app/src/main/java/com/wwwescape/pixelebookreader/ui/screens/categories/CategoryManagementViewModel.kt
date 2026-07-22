package com.wwwescape.pixelebookreader.ui.screens.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.data.category.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryManagementViewModel(application: Application) : AndroidViewModel(application) {

    val categories: StateFlow<List<Category>> = CategoryRepository.getAllCategories(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(title: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        val nextOrder = (categories.value.maxOfOrNull { it.order } ?: -1) + 1
        CategoryRepository.addCategory(getApplication(), Category(title = title.trim(), order = nextOrder))
    }

    fun renameCategory(category: Category, newTitle: String) = viewModelScope.launch {
        if (newTitle.isBlank()) return@launch
        CategoryRepository.updateCategory(getApplication(), category.copy(title = newTitle.trim()))
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        CategoryRepository.deleteCategory(getApplication(), category)
    }

    fun moveUp(category: Category) = swap(category, -1)

    fun moveDown(category: Category) = swap(category, 1)

    /** Reordering is up/down buttons rather than drag-and-drop, to avoid a new dependency for
     * what's otherwise a rarely-reordered list — see the note on `Category.order`. */
    private fun swap(category: Category, delta: Int) = viewModelScope.launch {
        val list = categories.value
        val index = list.indexOfFirst { it.id == category.id }
        val targetIndex = index + delta
        if (index < 0 || targetIndex < 0 || targetIndex >= list.size) return@launch
        val other = list[targetIndex]
        CategoryRepository.updateCategory(getApplication(), category.copy(order = other.order))
        CategoryRepository.updateCategory(getApplication(), other.copy(order = category.order))
    }
}

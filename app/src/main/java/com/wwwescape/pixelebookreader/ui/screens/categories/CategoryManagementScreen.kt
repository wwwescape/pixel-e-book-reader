package com.wwwescape.pixelebookreader.ui.screens.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.pixelebookreader.R
import com.wwwescape.pixelebookreader.data.category.Category
import com.wwwescape.pixelebookreader.ui.components.EmptyStateNotice

@Composable
fun CategoryManagementScreen(modifier: Modifier = Modifier, viewModel: CategoryManagementViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    var newCategoryTitle by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newCategoryTitle,
                onValueChange = { newCategoryTitle = it },
                placeholder = { Text(stringResource(R.string.category_new_placeholder)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                viewModel.addCategory(newCategoryTitle)
                newCategoryTitle = ""
            }) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = stringResource(R.string.action_add_category))
            }
        }

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                EmptyStateNotice(
                    title = stringResource(R.string.category_empty_title),
                    body = stringResource(R.string.category_empty_body),
                    icon = Icons.Rounded.Edit,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(categories, key = { it.id }) { category ->
                    val index = categories.indexOf(category)
                    ListItem(
                        headlineContent = { Text(category.title) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.moveUp(category) }, enabled = index > 0) {
                                    Icon(imageVector = Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.action_move_up))
                                }
                                IconButton(onClick = { viewModel.moveDown(category) }, enabled = index < categories.lastIndex) {
                                    Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.action_move_down))
                                }
                                IconButton(onClick = { editingCategory = category }) {
                                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_rename))
                                }
                                IconButton(onClick = { deletingCategory = category }) {
                                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    editingCategory?.let { category ->
        var title by remember(category.id) { mutableStateOf(category.title) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { viewModel.renameCategory(category, title); editingCategory = null }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    deletingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text(stringResource(R.string.category_delete_title)) },
            text = { Text(stringResource(R.string.category_delete_body, category.title)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(category); deletingCategory = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = { TextButton(onClick = { deletingCategory = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FileNode

@Composable
fun FileTreePanel(
    fileTree: List<FileNode>,
    onFileClick: (FileNode) -> Unit,
    onCreateFileInDir: (dirUri: Uri, fileName: String) -> Unit,
    onDeleteNode: (FileNode) -> Unit
) {
    var selectedNodeForMenu by remember { mutableStateOf<FileNode?>(null) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }

    if (showCreateFileDialog && selectedNodeForMenu != null) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Создать файл") },
            text = {
                OutlinedTextField(
                    value = newFileNameInput,
                    onValueChange = { newFileNameInput = it },
                    label = { Text("Имя файла (например, Utils.kt)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileNameInput.isNotBlank()) {
                            selectedNodeForMenu?.uri?.let { uri ->
                                onCreateFileInDir(uri, newFileNameInput.trim())
                            }
                            showCreateFileDialog = false
                            newFileNameInput = ""
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (selectedNodeForMenu != null && !showCreateFileDialog) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedNodeForMenu = null }
        ) {
            if (selectedNodeForMenu?.isDir == true) {
                DropdownMenuItem(
                    text = { Text("Создать файл внутри") },
                    onClick = {
                        showCreateFileDialog = true
                    },
                    leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null) }
                )
            }
            DropdownMenuItem(
                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    selectedNodeForMenu?.let { onDeleteNode(it) }
                    selectedNodeForMenu = null
                },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            )
        }
    }

    if (fileTree.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Папка пуста", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(fileTree) { node ->
                FileNodeItem(
                    node = node,
                    depth = 0,
                    onFileClick = onFileClick,
                    onLongClick = { selectedNodeForMenu = it },
                    onCreateFile = {
                        selectedNodeForMenu = it
                        showCreateFileDialog = true
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileNodeItem(
    node: FileNode,
    depth: Int,
    onFileClick: (FileNode) -> Unit,
    onLongClick: (FileNode) -> Unit,
    onCreateFile: (FileNode) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(start = (depth * 16).dp)
                .combinedClickable(
                    onClick = {
                        if (node.isDir) {
                            isExpanded = !isExpanded
                        } else {
                            onFileClick(node)
                        }
                    },
                    onLongClick = { onLongClick(node) }
                )
        ) {
            if (node.isDir) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    imageVector = getFileIcon(node.extension),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }

        if (node.isDir && isExpanded) {
            node.children.forEach { child ->
                FileNodeItem(
                    node = child,
                    depth = depth + 1,
                    onFileClick = onFileClick,
                    onLongClick = onLongClick,
                    onCreateFile = onCreateFile
                )
            }
        }
    }
}

private fun getFileIcon(extension: String) = when (extension) {
    "kt", "kts", "java" -> Icons.Outlined.Code
    "py" -> Icons.Outlined.Terminal
    "html", "css", "js" -> Icons.Outlined.Language
    "json", "xml" -> Icons.Outlined.DataObject
    "md" -> Icons.Outlined.Description
    else -> Icons.Outlined.InsertDriveFile
}

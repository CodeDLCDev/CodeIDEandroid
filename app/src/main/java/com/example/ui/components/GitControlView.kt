package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitCommitItem
import com.example.data.model.GitStatusItem

@Composable
fun GitControlView(
    gitStatusList: List<GitStatusItem>,
    commitHistory: List<GitCommitItem>,
    onCommit: (message: String) -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onClone: (url: String, targetFolder: String) -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }
    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneUrlInput by remember { mutableStateOf("") }
    var cloneFolderInput by remember { mutableStateOf("") }

    if (showCloneDialog) {
        AlertDialog(
            onDismissRequest = { showCloneDialog = false },
            title = { Text("Клонировать репозиторий") },
            text = {
                Column {
                    OutlinedTextField(
                        value = cloneUrlInput,
                        onValueChange = { cloneUrlInput = it },
                        label = { Text("URL репозитория (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cloneFolderInput,
                        onValueChange = { cloneFolderInput = it },
                        label = { Text("Имя папки назначения") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cloneUrlInput.isNotBlank() && cloneFolderInput.isNotBlank()) {
                            onClone(cloneUrlInput.trim(), cloneFolderInput.trim())
                            showCloneDialog = false
                            cloneUrlInput = ""
                            cloneFolderInput = ""
                        }
                    }
                ) {
                    Text("Клонировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloneDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // GIT HEADER & ACTIONS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Git Source Control",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = onPull) {
                    Icon(Icons.Outlined.Download, contentDescription = "Pull")
                }
                IconButton(onClick = onPush) {
                    Icon(Icons.Outlined.Upload, contentDescription = "Push")
                }
                OutlinedButton(onClick = { showCloneDialog = true }) {
                    Text("Clone", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // COMMIT SECTION
        OutlinedTextField(
            value = commitMessage,
            onValueChange = { commitMessage = it },
            label = { Text("Сообщение коммита...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Button disabled if message is blank OR no changes
        val canCommit = commitMessage.isNotBlank() && gitStatusList.isNotEmpty()
        Button(
            onClick = {
                if (canCommit) {
                    onCommit(commitMessage.trim())
                    commitMessage = ""
                }
            },
            enabled = canCommit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Commit Changes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHANGES LIST
        Text(
            text = "Changes (${gitStatusList.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (gitStatusList.isEmpty()) {
            Text(
                text = "Нет изменённых файлов",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
            ) {
                items(gitStatusList) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            color = when (item.statusBadge) {
                                "M" -> Color(0xFFF2C55C)
                                "A" -> Color(0xFF6AAB73)
                                "D" -> Color(0xFFFF6B68)
                                else -> Color(0xFF3574F0)
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.statusBadge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.filePath,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // COMMIT HISTORY
        Text(
            text = "Commit History",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(commitHistory) { rev ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rev.shortHash,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rev.date,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = rev.message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Author: ${rev.author}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

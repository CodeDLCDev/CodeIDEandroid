package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeIdeTopBar(
    projectName: String?,
    currentFileName: String?,
    onHamburgerClick: () -> Unit,
    onRunClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onAiClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onHamburgerClick) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Project Menu"
                )
            }
        },
        title = {
            Column {
                Text(
                    text = projectName ?: "CodeIDE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!currentFileName.isNullOrBlank()) {
                    Text(
                        text = currentFileName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onRunClick) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Run/Build",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh Files"
                )
            }
            IconButton(onClick = onAiClick) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Open AI Panel",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

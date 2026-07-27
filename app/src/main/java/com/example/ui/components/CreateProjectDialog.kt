package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (projectName: String, templateType: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("KOTLIN") }
    var showError by remember { mutableStateOf(false) }

    val templates = listOf(
        "EMPTY" to "Пустая папка",
        "KOTLIN" to "Kotlin приложение",
        "PYTHON" to "Python скрипт",
        "WEB" to "Web (HTML/CSS/JS)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать новый проект", fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        showError = false
                    },
                    label = { Text("Название проекта") },
                    isError = showError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = "Введите название проекта",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Выберите шаблон:", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    templates.forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .selectable(
                                    selected = (selectedTemplate == type),
                                    onClick = { selectedTemplate = type },
                                    role = Role.RadioButton
                                )
                        ) {
                            RadioButton(
                                selected = (selectedTemplate == type),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isBlank()) {
                        showError = true
                    } else {
                        onCreate(projectName.trim(), selectedTemplate)
                    }
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SettingsState

@Composable
fun SettingsView(
    settings: SettingsState,
    onFontSizeChange: (Float) -> Unit,
    onShowLineNumbersChange: (Boolean) -> Unit,
    onWordWrapChange: (Boolean) -> Unit,
    onAutoIndentationChange: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiEndpointChange: (String) -> Unit,
    onGitProfileChange: (userName: String, email: String, token: String) -> Unit,
    onTestAiConnection: ((String) -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()

    var apiKeyInput by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var apiEndpointInput by remember(settings.apiEndpoint) { mutableStateOf(settings.apiEndpoint) }
    var gitUserNameInput by remember(settings.gitUserName) { mutableStateOf(settings.gitUserName) }
    var gitEmailInput by remember(settings.gitEmail) { mutableStateOf(settings.gitEmail) }
    var gitTokenInput by remember(settings.gitToken) { mutableStateOf(settings.gitToken) }

    var testConnectionResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки CodeIDE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: EDITOR
        SettingsHeader("Редактор кода")

        Text("Размер шрифта: ${settings.fontSize.toInt()} sp", fontSize = 14.sp)
        Slider(
            value = settings.fontSize,
            onValueChange = onFontSizeChange,
            valueRange = 8f..24f,
            steps = 15,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsToggleRow(
            label = "Нумерация строк (Show Line Numbers)",
            checked = settings.showLineNumbers,
            onCheckedChange = onShowLineNumbersChange
        )

        SettingsToggleRow(
            label = "Перенос слов (Word Wrap)",
            checked = settings.wordWrap,
            onCheckedChange = onWordWrapChange
        )

        SettingsToggleRow(
            label = "Автоотступы (Auto Indentation)",
            checked = settings.autoIndentation,
            onCheckedChange = onAutoIndentationChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: THEMES
        SettingsHeader("Темы и внешний вид")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onThemeModeChange("darcula") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (settings.themeMode == "darcula") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("IntelliJ Darcula")
            }

            Button(
                onClick = { onThemeModeChange("light") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (settings.themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("IntelliJ Light")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: OPENCODE AI
        SettingsHeader("OpenCode AI Assistant")

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = {
                apiKeyInput = it
                onApiKeyChange(it)
            },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apiEndpointInput,
            onValueChange = {
                apiEndpointInput = it
                onApiEndpointChange(it)
            },
            label = { Text("Endpoint URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onTestAiConnection { res ->
                    testConnectionResult = res
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Проверить соединение")
        }

        if (testConnectionResult != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = testConnectionResult ?: "",
                fontSize = 12.sp,
                color = if (testConnectionResult?.contains("Error") == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 4: GIT
        SettingsHeader("Профиль Git")

        OutlinedTextField(
            value = gitUserNameInput,
            onValueChange = {
                gitUserNameInput = it
                onGitProfileChange(gitUserNameInput, gitEmailInput, gitTokenInput)
            },
            label = { Text("Имя пользователя Git") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = gitEmailInput,
            onValueChange = {
                gitEmailInput = it
                onGitProfileChange(gitUserNameInput, gitEmailInput, gitTokenInput)
            },
            label = { Text("Email Git") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = gitTokenInput,
            onValueChange = {
                gitTokenInput = it
                onGitProfileChange(gitUserNameInput, gitEmailInput, gitTokenInput)
            },
            label = { Text("Personal Access Token (for Push/Pull)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

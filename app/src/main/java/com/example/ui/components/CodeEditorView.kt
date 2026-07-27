package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SettingsState
import com.example.data.model.EditorTab
import com.example.parser.CodeTokenizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorView(
    openTabs: List<EditorTab>,
    activeTabIndex: Int,
    settings: SettingsState,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSendSelectionToAi: (selectedText: String) -> Unit
) {
    if (openTabs.isEmpty() || activeTabIndex !in openTabs.indices) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Нет открытых файлов",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Выберите файл из дерева слева для редактирования",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val activeTab = openTabs[activeTabIndex]
    var textFieldValue by remember(activeTab.id, activeTab.content) {
        mutableStateOf(TextFieldValue(activeTab.content))
    }

    var showSearchReplaceBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TABS HEADER BAR
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            itemsIndexed(openTabs) { idx, tab ->
                val isSelected = idx == activeTabIndex
                Surface(
                    onClick = { onTabSelect(idx) },
                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (tab.isDirty) "${tab.fileName} *" else tab.fileName,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onTabClose(idx) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close tab",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // TOOLBAR FOR SAVE / SEARCH / AI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSaveClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Save file",
                        tint = if (activeTab.isDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { showSearchReplaceBar = !showSearchReplaceBar }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search and replace",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (textFieldValue.selection.length > 0) {
                TextButton(
                    onClick = {
                        val selectedText = textFieldValue.text.substring(
                            textFieldValue.selection.start,
                            textFieldValue.selection.end
                        )
                        onSendSelectionToAi(selectedText)
                    },
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отправить в AI", fontSize = 11.sp)
                }
            }
        }

        // SEARCH AND REPLACE BAR
        if (showSearchReplaceBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск...") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    placeholder = { Text("Замена...") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            val newText = textFieldValue.text.replace(searchQuery, replaceQuery)
                            textFieldValue = TextFieldValue(newText)
                            onContentChange(newText)
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Заменить всё", fontSize = 11.sp)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // EDITOR CANVAS
        val lines = remember(textFieldValue.text) { textFieldValue.text.split("\n") }
        val highlightedAnnotatedString = remember(textFieldValue.text, activeTab.language, isDark) {
            CodeTokenizer.highlightCode(textFieldValue.text, activeTab.language, isDark)
        }

        val textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = settings.fontSize.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // LINE NUMBERS COLUMN (REACTIVE)
            if (settings.showLineNumbers) {
                LazyColumn(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    itemsIndexed(lines) { idx, _ ->
                        Text(
                            text = "${idx + 1}",
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = (settings.fontSize * 0.85f).sp
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }

            // TEXT INPUT CANVAS
            val horizontalScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!settings.wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier
                    )
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        // Handle Auto Indentation
                        val oldText = textFieldValue.text
                        var finalValue = newValue

                        if (settings.autoIndentation && newValue.text.length == oldText.length + 1 && newValue.text.endsWith("\n")) {
                            val lastLine = oldText.substringBeforeLast("\n", oldText)
                            val indent = lastLine.takeWhile { it == ' ' || it == '\t' }
                            if (indent.isNotEmpty()) {
                                val addedIndent = if (lastLine.endsWith("{")) "$indent    " else indent
                                val newText = newValue.text + addedIndent
                                finalValue = TextFieldValue(newText, selection = TextRange(newText.length))
                            }
                        }

                        textFieldValue = finalValue
                        onContentChange(finalValue.text)
                    },
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxSize(),
                    visualTransformation = {
                        androidx.compose.ui.text.input.TransformedText(
                            text = highlightedAnnotatedString,
                            offsetMapping = androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    }
                )
            }
        }
    }
}

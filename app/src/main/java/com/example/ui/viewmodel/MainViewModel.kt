package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.OpenCodeService
import com.example.data.local.*
import com.example.data.model.*
import com.example.git.GitManager
import com.example.parser.CodeAnalyzer
import com.example.terminal.TerminalRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()
    private val terminalDao = db.terminalCommandDao()
    private val chatDao = db.chatMessageDao()
    private val settingsDataStore = SettingsDataStore(application)

    private val gitManager = GitManager()
    private val openCodeService = OpenCodeService()
    private val terminalRunner = TerminalRunner()

    // Settings State
    val settingsState: StateFlow<SettingsState> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    // Projects list from Room
    val projectsList: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project
    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Project File Tree
    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    // Editor Tabs
    private val _openTabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val openTabs: StateFlow<List<EditorTab>> = _openTabs.asStateFlow()

    private val _activeTabIndex = MutableStateFlow(0)
    val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

    // Code Diagnostics (Problems)
    private val _diagnostics = MutableStateFlow<List<DiagnosticItem>>(emptyList())
    val diagnostics: StateFlow<List<DiagnosticItem>> = _diagnostics.asStateFlow()

    // AI Chat Messages
    private val _chatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Ai Diff Proposal State for Apply/Reject
    private val _aiDiffProposal = MutableStateFlow<Pair<String, String>?>(null) // (targetFileName, proposedCode)
    val aiDiffProposal: StateFlow<Pair<String, String>?> = _aiDiffProposal.asStateFlow()

    // Terminal State
    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    val terminalHistory: StateFlow<List<TerminalCommandEntity>> = terminalDao.getRecentCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Git State
    private val _gitStatusList = MutableStateFlow<List<GitStatusItem>>(emptyList())
    val gitStatusList: StateFlow<List<GitStatusItem>> = _gitStatusList.asStateFlow()

    private val _gitCommitHistory = MutableStateFlow<List<GitCommitItem>>(emptyList())
    val gitCommitHistory: StateFlow<List<GitCommitItem>> = _gitCommitHistory.asStateFlow()

    private val _gitMessage = MutableStateFlow<String?>(null)
    val gitMessage: StateFlow<String?> = _gitMessage.asStateFlow()

    // UI Feedback Message (Snackbar)
    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    // Active Bottom Navigation Tab: 0=Editor, 1=Problems, 2=AI, 3=Git, 4=Terminal, 5=Settings
    private val _bottomNavTab = MutableStateFlow(0)
    val bottomNavTab: StateFlow<Int> = _bottomNavTab.asStateFlow()

    fun selectBottomNavTab(index: Int) {
        _bottomNavTab.value = index
    }

    // --- PROJECT MANAGEMENT ---

    fun openProjectByUri(treeUri: Uri, contextName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if already held or non-persistable
            }

            val docFile = DocumentFile.fromTreeUri(getApplication(), treeUri)
            val projName = contextName ?: docFile?.name ?: "Project"

            val existing = projectDao.getProjectByUri(treeUri.toString())
            val entity = if (existing != null) {
                val updated = existing.copy(lastOpenedTimestamp = System.currentTimeMillis())
                projectDao.updateProject(updated)
                updated
            } else {
                val newEntity = ProjectEntity(
                    name = projName,
                    treeUri = treeUri.toString(),
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
                val newId = projectDao.insertProject(newEntity)
                newEntity.copy(id = newId)
            }

            _activeProject.value = entity
            scanProjectFiles(docFile)
            refreshGitStatus()
            loadChatHistoryForProject(entity.id)
            _snackMessage.value = "Opened project: $projName"
        }
    }

    fun createNewProject(parentTreeUri: Uri, projectName: String, templateType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = getApplication<Application>().contentResolver
            try {
                contentResolver.takePersistableUriPermission(
                    parentTreeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {}

            val parentDoc = DocumentFile.fromTreeUri(getApplication(), parentTreeUri)
                ?: return@launch

            val projDir = parentDoc.createDirectory(projectName) ?: return@launch

            // Create initial files based on template
            when (templateType) {
                "EMPTY" -> {
                    val readme = projDir.createFile("text/markdown", "README.md")
                    writeDocContent(readme, "# $projectName\nCreated with CodeIDE.")
                }
                "KOTLIN" -> {
                    val gradleFile = projDir.createFile("text/plain", "build.gradle.kts")
                    writeDocContent(
                        gradleFile,
                        "plugins {\n    kotlin(\"jvm\") version \"2.2.10\"\n}\n\nrepositories {\n    mavenCentral()\n}\n"
                    )
                    val srcDir = projDir.createDirectory("src")?.createDirectory("main")?.createDirectory("kotlin")
                    val mainFile = srcDir?.createFile("text/plain", "Main.kt")
                    writeDocContent(mainFile, "fun main() {\n    println(\"Hello, CodeIDE!\")\n}\n")
                }
                "PYTHON" -> {
                    val mainPy = projDir.createFile("text/plain", "main.py")
                    writeDocContent(mainPy, "# Python script\nprint(\"Hello, CodeIDE!\")\n")
                }
                "WEB" -> {
                    val html = projDir.createFile("text/html", "index.html")
                    writeDocContent(
                        html,
                        "<!DOCTYPE html>\n<html>\n<head>\n    <title>$projectName</title>\n    <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n    <h1>Hello from CodeIDE</h1>\n    <script src=\"script.js\"></script>\n</body>\n</html>"
                    )
                    val css = projDir.createFile("text/css", "style.css")
                    writeDocContent(css, "body {\n    font-family: sans-serif;\n    background-color: #1e1f22;\n    color: #dfe1e5;\n    padding: 20px;\n}")
                    val js = projDir.createFile("text/javascript", "script.js")
                    writeDocContent(js, "console.log('Script loaded successfully');")
                }
            }

            openProjectByUri(projDir.uri, projectName)
        }
    }

    fun deleteRecentProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            projectDao.deleteProjectById(project.id)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _fileTree.value = emptyList()
                _openTabs.value = emptyList()
            }
            _snackMessage.value = "Removed from recent projects"
        }
    }

    private fun scanProjectFiles(parentDoc: DocumentFile?) {
        if (parentDoc == null) return
        val nodes = buildFileTreeNodes(parentDoc)
        _fileTree.value = nodes
    }

    private fun buildFileTreeNodes(doc: DocumentFile): List<FileNode> {
        val files = doc.listFiles()
        return files.mapNotNull { file ->
            val name = file.name ?: return@mapNotNull null
            // Basic .gitignore style filtering
            if (name == "build" || name == ".gradle" || name.endsWith(".class")) return@mapNotNull null

            if (file.isDirectory) {
                FileNode(
                    name = name,
                    uri = file.uri,
                    isDir = true,
                    path = file.uri.toString(),
                    children = buildFileTreeNodes(file)
                )
            } else {
                FileNode(
                    name = name,
                    uri = file.uri,
                    isDir = false,
                    path = file.uri.toString()
                )
            }
        }.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
    }

    fun refreshFileTree() {
        viewModelScope.launch(Dispatchers.IO) {
            val active = _activeProject.value ?: return@launch
            val uri = Uri.parse(active.treeUri)
            val docFile = DocumentFile.fromTreeUri(getApplication(), uri)
            scanProjectFiles(docFile)
            refreshGitStatus()
            _snackMessage.value = "Project files updated"
        }
    }

    // --- FILE OPERATIONS & EDITOR ---

    fun openFileInTab(node: FileNode) {
        if (node.isDir) return
        viewModelScope.launch(Dispatchers.IO) {
            val existingIndex = _openTabs.value.indexOfFirst { it.id == node.uri.toString() }
            if (existingIndex >= 0) {
                _activeTabIndex.value = existingIndex
                return@launch
            }

            val content = readDocContent(node.uri)
            val lang = when (node.extension) {
                "kt", "kts" -> "kotlin"
                "java" -> "java"
                "py" -> "python"
                "js", "ts" -> "javascript"
                "html" -> "html"
                "css" -> "css"
                "xml" -> "xml"
                "json" -> "json"
                "md" -> "markdown"
                else -> "txt"
            }

            val tab = EditorTab(
                id = node.uri.toString(),
                fileName = node.name,
                fileUri = node.uri,
                content = content,
                originalContent = content,
                language = lang
            )

            val updatedList = _openTabs.value.toMutableList().apply { add(tab) }
            _openTabs.value = updatedList
            _activeTabIndex.value = updatedList.size - 1

            analyzeActiveTabCode(tab)
        }
    }

    fun updateActiveTabContent(newContent: String) {
        val tabs = _openTabs.value.toMutableList()
        val index = _activeTabIndex.value
        if (index in tabs.indices) {
            val current = tabs[index]
            val updated = current.copy(content = newContent)
            tabs[index] = updated
            _openTabs.value = tabs
            analyzeActiveTabCode(updated)
        }
    }

    fun saveActiveTab() {
        val tabs = _openTabs.value
        val index = _activeTabIndex.value
        if (index in tabs.indices) {
            val current = tabs[index]
            viewModelScope.launch(Dispatchers.IO) {
                writeDocContentByUri(current.fileUri, current.content)
                val updatedTabs = _openTabs.value.toMutableList()
                if (index in updatedTabs.indices) {
                    updatedTabs[index] = current.copy(originalContent = current.content)
                    _openTabs.value = updatedTabs
                }
                _snackMessage.value = "Saved ${current.fileName}"
                refreshGitStatus()
            }
        }
    }

    fun closeTab(tabIndex: Int) {
        val tabs = _openTabs.value.toMutableList()
        if (tabIndex in tabs.indices) {
            tabs.removeAt(tabIndex)
            _openTabs.value = tabs
            if (_activeTabIndex.value >= tabs.size) {
                _activeTabIndex.value = (tabs.size - 1).coerceAtLeast(0)
            }
        }
    }

    fun createNewFileInDirectory(dirUri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = DocumentFile.fromTreeUri(getApplication(), dirUri) ?: return@launch
            val newFile = doc.createFile("text/plain", fileName)
            if (newFile != null) {
                _snackMessage.value = "Created file $fileName"
                refreshFileTree()
            }
        }
    }

    fun deleteFileNode(node: FileNode) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = DocumentFile.fromSingleUri(getApplication(), node.uri)
                ?: DocumentFile.fromTreeUri(getApplication(), node.uri)
            if (doc != null && doc.delete()) {
                _snackMessage.value = "Deleted ${node.name}"
                // Close tab if open
                val openIdx = _openTabs.value.indexOfFirst { it.id == node.uri.toString() }
                if (openIdx >= 0) {
                    closeTab(openIdx)
                }
                refreshFileTree()
            }
        }
    }

    private fun analyzeActiveTabCode(tab: EditorTab) {
        val results = CodeAnalyzer.analyzeCode(tab.fileName, tab.fileUri, tab.content)
        _diagnostics.value = results
    }

    private fun readDocContent(uri: Uri): String {
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: ""
        } catch (e: Exception) {
            "// Could not read file content: ${e.localizedMessage}"
        }
    }

    private fun writeDocContent(doc: DocumentFile?, content: String) {
        if (doc == null) return
        writeDocContentByUri(doc.uri, content)
    }

    private fun writeDocContentByUri(uri: Uri, content: String) {
        try {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.bufferedWriter().write(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SETTINGS CONTROLS ---

    fun setFontSize(size: Float) {
        viewModelScope.launch { settingsDataStore.updateFontSize(size) }
    }

    fun setShowLineNumbers(show: Boolean) {
        viewModelScope.launch { settingsDataStore.updateShowLineNumbers(show) }
    }

    fun setWordWrap(wrap: Boolean) {
        viewModelScope.launch { settingsDataStore.updateWordWrap(wrap) }
    }

    fun setAutoIndentation(auto: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoIndentation(auto) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsDataStore.updateThemeMode(mode) }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch { settingsDataStore.updateApiKey(key) }
    }

    fun setApiEndpoint(endpoint: String) {
        viewModelScope.launch { settingsDataStore.updateApiEndpoint(endpoint) }
    }

    fun setGitProfile(userName: String, email: String, token: String) {
        viewModelScope.launch { settingsDataStore.updateGitProfile(userName, email, token) }
    }

    fun testAiConnection(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val settings = settingsState.value
            val result = openCodeService.testConnection(settings.apiEndpoint, settings.apiKey)
            if (result.isSuccess) {
                onResult(result.getOrDefault("Connection Success"))
            } else {
                onResult("Connection Error: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    // --- OPENCODE AI CHAT ---

    private fun loadChatHistoryForProject(projectId: Long) {
        viewModelScope.launch {
            chatDao.getMessagesForProject(projectId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun sendAiMessage(prompt: String, codeSnippet: String? = null) {
        val projId = _activeProject.value?.id ?: 0L
        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                projectId = projId,
                role = "user",
                content = prompt,
                codeSnippet = codeSnippet
            )
            chatDao.insertMessage(userMsg)

            val settings = settingsState.value
            if (settings.apiKey.isBlank()) {
                val alertMsg = ChatMessageEntity(
                    projectId = projId,
                    role = "assistant",
                    content = "⚠️ API key is missing. Please configure your API key in OpenCode AI settings."
                )
                chatDao.insertMessage(alertMsg)
                return@launch
            }

            _isAiLoading.value = true
            val result = openCodeService.sendMessage(
                endpoint = settings.apiEndpoint,
                apiKey = settings.apiKey,
                userPrompt = prompt,
                codeSnippet = codeSnippet
            )
            _isAiLoading.value = false

            if (result.isSuccess) {
                val text = result.getOrDefault("")
                val assistantMsg = ChatMessageEntity(
                    projectId = projId,
                    role = "assistant",
                    content = text
                )
                chatDao.insertMessage(assistantMsg)

                // Check if response contains code proposed for editing active file
                if (text.contains("```")) {
                    val codeBlock = text.substringAfter("```").substringAfter("\n").substringBefore("```")
                    val currentTab = openTabs.value.getOrNull(activeTabIndex.value)
                    if (currentTab != null) {
                        _aiDiffProposal.value = currentTab.fileName to codeBlock
                    }
                }
            } else {
                val errText = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                val assistantMsg = ChatMessageEntity(
                    projectId = projId,
                    role = "assistant",
                    content = "❌ Error: $errText"
                )
                chatDao.insertMessage(assistantMsg)
            }
        }
    }

    fun applyAiDiffProposal() {
        val proposal = _aiDiffProposal.value ?: return
        val currentTab = openTabs.value.getOrNull(activeTabIndex.value)
        if (currentTab != null && currentTab.fileName == proposal.first) {
            updateActiveTabContent(proposal.second)
            _snackMessage.value = "Applied AI changes to ${currentTab.fileName}"
        }
        _aiDiffProposal.value = null
    }

    fun rejectAiDiffProposal() {
        _aiDiffProposal.value = null
    }

    // --- TERMINAL ---

    fun runTerminalCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            terminalDao.insertCommand(TerminalCommandEntity(command = command))

            val currentLog = _terminalLogs.value.toMutableList()
            currentLog.add("$ $command")
            _terminalLogs.value = currentLog

            val result = terminalRunner.executeCommand(command, getActiveProjectDir())

            val updatedLog = _terminalLogs.value.toMutableList()
            if (result.output.isNotEmpty()) {
                updatedLog.add(result.output)
            }
            if (result.error.isNotEmpty()) {
                updatedLog.add("[stderr] ${result.error}")
            }
            if (result.exitCode != 0) {
                updatedLog.add("[process exited with code ${result.exitCode}]")
            }
            _terminalLogs.value = updatedLog
        }
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    // --- GIT INTEGRATION ---

    private fun getActiveProjectDir(): File? {
        val proj = _activeProject.value ?: return null
        val uri = Uri.parse(proj.treeUri)
        val path = uri.path
        return if (path != null) File(path) else null
    }

    fun refreshGitStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = getActiveProjectDir() ?: return@launch
            val status = gitManager.getStatus(dir)
            _gitStatusList.value = status

            val history = gitManager.getCommitHistory(dir)
            _gitCommitHistory.value = history
        }
    }

    fun gitCommit(message: String) {
        val dir = getActiveProjectDir()
        if (dir == null) {
            _snackMessage.value = "No active project directory"
            return
        }
        val settings = settingsState.value
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitManager.commit(dir, message, settings.gitUserName, settings.gitEmail)
            if (res.isSuccess) {
                _snackMessage.value = res.getOrNull()
                refreshGitStatus()
            } else {
                _snackMessage.value = "Git commit failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun gitPush() {
        val dir = getActiveProjectDir() ?: return
        val settings = settingsState.value
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitManager.push(dir, settings.gitToken)
            _snackMessage.value = if (res.isSuccess) res.getOrNull() else "Push failed: ${res.exceptionOrNull()?.localizedMessage}"
        }
    }

    fun gitPull() {
        val dir = getActiveProjectDir() ?: return
        val settings = settingsState.value
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitManager.pull(dir, settings.gitToken)
            if (res.isSuccess) {
                _snackMessage.value = res.getOrNull()
                refreshFileTree()
            } else {
                _snackMessage.value = "Pull failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun gitClone(url: String, targetFolderName: String) {
        val active = _activeProject.value ?: return
        val dir = getActiveProjectDir() ?: return
        val targetDir = File(dir, targetFolderName)
        val settings = settingsState.value
        viewModelScope.launch(Dispatchers.IO) {
            val res = gitManager.cloneRepository(url, targetDir, settings.gitToken)
            if (res.isSuccess) {
                _snackMessage.value = res.getOrNull()
                refreshFileTree()
            } else {
                _snackMessage.value = "Clone failed: ${res.exceptionOrNull()?.localizedMessage}"
            }
        }
    }
}

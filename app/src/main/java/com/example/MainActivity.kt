package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FileNode
import com.example.ui.components.*
import com.example.ui.theme.CodeIDETheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val recentProjects by viewModel.projectsList.collectAsStateWithLifecycle()
            val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
            val fileTree by viewModel.fileTree.collectAsStateWithLifecycle()
            val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
            val activeTabIndex by viewModel.activeTabIndex.collectAsStateWithLifecycle()
            val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
            val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
            val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
            val aiDiffProposal by viewModel.aiDiffProposal.collectAsStateWithLifecycle()
            val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
            val gitStatusList by viewModel.gitStatusList.collectAsStateWithLifecycle()
            val gitCommitHistory by viewModel.gitCommitHistory.collectAsStateWithLifecycle()
            val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()
            val selectedBottomTab by viewModel.bottomNavTab.collectAsStateWithLifecycle()

            val isDark = settings.themeMode == "darcula"

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            var showCreateProjectDialog by remember { mutableStateOf(false) }
            var pendingCreateProjectParams by remember { mutableStateOf<Pair<String, String>?>(null) } // (name, template)

            // SAF Launchers
            val openProjectLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    viewModel.openProjectByUri(uri)
                }
            }

            val createProjectFolderLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null && pendingCreateProjectParams != null) {
                    val (pName, pTemplate) = pendingCreateProjectParams!!
                    viewModel.createNewProject(uri, pName, pTemplate)
                    pendingCreateProjectParams = null
                }
            }

            // Snackbar listener
            LaunchedEffect(snackMessage) {
                snackMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearSnackMessage()
                }
            }

            CodeIDETheme(isDarkTheme = isDark) {
                if (showCreateProjectDialog) {
                    CreateProjectDialog(
                        onDismiss = { showCreateProjectDialog = false },
                        onCreate = { name, template ->
                            showCreateProjectDialog = false
                            pendingCreateProjectParams = name to template
                            createProjectFolderLauncher.launch(null)
                        }
                    )
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ProjectDrawerContent(
                            recentProjects = recentProjects,
                            activeProject = activeProject,
                            onCreateProjectClick = { showCreateProjectDialog = true },
                            onOpenProjectClick = { openProjectLauncher.launch(null) },
                            onProjectSelect = { proj -> viewModel.openProjectByUri(Uri.parse(proj.treeUri), proj.name) },
                            onDeleteProject = { viewModel.deleteRecentProject(it) },
                            onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                        )
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            val activeTabName = openTabs.getOrNull(activeTabIndex)?.fileName
                            CodeIdeTopBar(
                                projectName = activeProject?.name,
                                currentFileName = activeTabName,
                                onHamburgerClick = { coroutineScope.launch { drawerState.open() } },
                                onRunClick = {
                                    viewModel.selectBottomNavTab(4) // Switch to Terminal
                                    viewModel.runTerminalCommand("echo 'Executing build task...'")
                                },
                                onRefreshClick = { viewModel.refreshFileTree() },
                                onAiClick = { viewModel.selectBottomNavTab(2) } // Switch to AI
                            )
                        },
                        bottomBar = {
                            CodeIdeBottomBar(
                                selectedTab = selectedBottomTab,
                                onTabSelected = { viewModel.selectBottomNavTab(it) }
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            if (activeProject == null) {
                                // EMPTY STATE WHEN NO PROJECT IS OPEN
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Нет открытого проекта",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Выберите существующий проект или создайте новый",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Button(
                                            onClick = { showCreateProjectDialog = true },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) {
                                            Icon(Icons.Outlined.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Создать новый проект")
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedButton(
                                            onClick = { openProjectLauncher.launch(null) },
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) {
                                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Открыть проект")
                                        }
                                    }
                                }
                            } else {
                                // ACTIVE PROJECT WORKSPACE
                                when (selectedBottomTab) {
                                    0 -> { // Editor & File Tree
                                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                            if (maxWidth > 600.dp) {
                                                // Dual pane layout on wide screens
                                                Row(modifier = Modifier.fillMaxSize()) {
                                                    Box(modifier = Modifier.width(260.dp).fillMaxHeight()) {
                                                        FileTreePanel(
                                                            fileTree = fileTree,
                                                            onFileClick = { viewModel.openFileInTab(it) },
                                                            onCreateFileInDir = { dirUri, fName -> viewModel.createNewFileInDirectory(dirUri, fName) },
                                                            onDeleteNode = { viewModel.deleteFileNode(it) }
                                                        )
                                                    }
                                                    VerticalDivider()
                                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                        CodeEditorView(
                                                            openTabs = openTabs,
                                                            activeTabIndex = activeTabIndex,
                                                            settings = settings,
                                                            onTabSelect = { viewModel.openFileInTab(fileTree.first()) }, // or tab switch
                                                            onTabClose = { viewModel.closeTab(it) },
                                                            onContentChange = { viewModel.updateActiveTabContent(it) },
                                                            onSaveClick = { viewModel.saveActiveTab() },
                                                            onSendSelectionToAi = { snippet ->
                                                                viewModel.selectBottomNavTab(2)
                                                                viewModel.sendAiMessage("Explain or improve this code snippet:", snippet)
                                                            }
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Single pane on phone: Editor as primary, File tree accessible via Drawer or toggle
                                                CodeEditorView(
                                                    openTabs = openTabs,
                                                    activeTabIndex = activeTabIndex,
                                                    settings = settings,
                                                    onTabSelect = { idx -> viewModel.openFileInTab(FileNode(openTabs[idx].fileName, openTabs[idx].fileUri, false)) },
                                                    onTabClose = { viewModel.closeTab(it) },
                                                    onContentChange = { viewModel.updateActiveTabContent(it) },
                                                    onSaveClick = { viewModel.saveActiveTab() },
                                                    onSendSelectionToAi = { snippet ->
                                                        viewModel.selectBottomNavTab(2)
                                                        viewModel.sendAiMessage("Explain or improve this code snippet:", snippet)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    1 -> { // Problems
                                        ProblemsView(
                                            diagnostics = diagnostics,
                                            onDiagnosticClick = { diag ->
                                                viewModel.selectBottomNavTab(0)
                                            }
                                        )
                                    }
                                    2 -> { // OpenCode AI
                                        OpenCodeChatView(
                                            messages = chatMessages,
                                            isLoading = isAiLoading,
                                            aiDiffProposal = aiDiffProposal,
                                            onSendMessage = { prompt, snippet -> viewModel.sendAiMessage(prompt, snippet) },
                                            onApplyDiff = { viewModel.applyAiDiffProposal() },
                                            onRejectDiff = { viewModel.rejectAiDiffProposal() }
                                        )
                                    }
                                    3 -> { // Git
                                        GitControlView(
                                            gitStatusList = gitStatusList,
                                            commitHistory = gitCommitHistory,
                                            onCommit = { viewModel.gitCommit(it) },
                                            onPush = { viewModel.gitPush() },
                                            onPull = { viewModel.gitPull() },
                                            onClone = { url, folder -> viewModel.gitClone(url, folder) }
                                        )
                                    }
                                    4 -> { // Terminal
                                        TerminalView(
                                            terminalLogs = terminalLogs,
                                            onRunCommand = { viewModel.runTerminalCommand(it) },
                                            onClearLogs = { viewModel.clearTerminalLogs() }
                                        )
                                    }
                                    5 -> { // Settings
                                        SettingsView(
                                            settings = settings,
                                            onFontSizeChange = { viewModel.setFontSize(it) },
                                            onShowLineNumbersChange = { viewModel.setShowLineNumbers(it) },
                                            onWordWrapChange = { viewModel.setWordWrap(it) },
                                            onAutoIndentationChange = { viewModel.setAutoIndentation(it) },
                                            onThemeModeChange = { viewModel.setThemeMode(it) },
                                            onApiKeyChange = { viewModel.setApiKey(it) },
                                            onApiEndpointChange = { viewModel.setApiEndpoint(it) },
                                            onGitProfileChange = { u, e, t -> viewModel.setGitProfile(u, e, t) },
                                            onTestAiConnection = { callback -> viewModel.testAiConnection(callback) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

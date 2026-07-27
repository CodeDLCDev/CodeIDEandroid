package com.example.data.model

import android.net.Uri

enum class GitStatusType {
    MODIFIED, ADDED, DELETED, UNTRACKED, NONE
}

data class FileNode(
    val name: String,
    val uri: Uri,
    val isDir: Boolean,
    val path: String = "",
    val children: List<FileNode> = emptyList(),
    val isExpanded: Boolean = false,
    val gitStatus: GitStatusType = GitStatusType.NONE
) {
    val extension: String
        get() = if (name.contains(".")) name.substringAfterLast(".").lowercase() else ""
}

data class EditorTab(
    val id: String, // uri.toString()
    val fileName: String,
    val fileUri: Uri,
    val content: String,
    val originalContent: String,
    val isDirty: Boolean = content != originalContent,
    val language: String,
    val cursorPosition: Int = 0,
    val scrollLine: Int = 0
)

enum class DiagnosticSeverity {
    ERROR, WARNING
}

data class DiagnosticItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val fileUri: Uri,
    val line: Int,
    val column: Int = 1,
    val message: String,
    val severity: DiagnosticSeverity,
    val quickFix: String? = null
)

data class GitStatusItem(
    val filePath: String,
    val status: GitStatusType,
    val statusBadge: String // "M", "A", "D", "?"
)

data class GitCommitItem(
    val hash: String,
    val shortHash: String,
    val author: String,
    val message: String,
    val date: String
)

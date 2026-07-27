package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val treeUri: String,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val templateType: String = "EMPTY"
)

@Entity(tableName = "terminal_history")
data class TerminalCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val workingDir: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val codeSnippet: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

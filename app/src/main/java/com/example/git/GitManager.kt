package com.example.git

import com.example.data.model.GitCommitItem
import com.example.data.model.GitStatusItem
import com.example.data.model.GitStatusType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class GitManager {

    fun getStatus(projectDir: File): List<GitStatusItem> {
        if (!projectDir.exists() || !File(projectDir, ".git").exists()) {
            return emptyList()
        }
        return try {
            Git.open(projectDir).use { git ->
                val status = git.status().call()
                val list = mutableListOf<GitStatusItem>()

                status.modified.forEach { list.add(GitStatusItem(it, GitStatusType.MODIFIED, "M")) }
                status.added.forEach { list.add(GitStatusItem(it, GitStatusType.ADDED, "A")) }
                status.uncommittedChanges.forEach {
                    if (!list.any { item -> item.filePath == it }) {
                        list.add(GitStatusItem(it, GitStatusType.MODIFIED, "M"))
                    }
                }
                status.untracked.forEach { list.add(GitStatusItem(it, GitStatusType.UNTRACKED, "?")) }
                status.missing.forEach { list.add(GitStatusItem(it, GitStatusType.DELETED, "D")) }

                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun commit(
        projectDir: File,
        message: String,
        authorName: String,
        authorEmail: String
    ): Result<String> {
        if (!File(projectDir, ".git").exists()) {
            // Init git repo if not existing
            try {
                Git.init().setDirectory(projectDir).call()
            } catch (e: Exception) {
                return Result.failure(Exception("Failed to init git repository: ${e.localizedMessage}"))
            }
        }
        return try {
            Git.open(projectDir).use { git ->
                git.add().addFilepattern(".").call()
                val commit = git.commit()
                    .setMessage(message)
                    .setAuthor(authorName, authorEmail)
                    .setCommitter(authorName, authorEmail)
                    .call()
                Result.success("Committed successfully: ${commit.name.take(7)}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCommitHistory(projectDir: File): List<GitCommitItem> {
        if (!File(projectDir, ".git").exists()) return emptyList()
        return try {
            Git.open(projectDir).use { git ->
                val logs = git.log().setMaxCount(20).call()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                logs.map { rev ->
                    GitCommitItem(
                        hash = rev.name,
                        shortHash = rev.name.take(7),
                        author = rev.authorIdent.name ?: "Unknown",
                        message = rev.shortMessage ?: "",
                        date = dateFormat.format(rev.commitTime * 1000L)
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun cloneRepository(remoteUrl: String, targetDir: File, token: String): Result<String> {
        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(targetDir)
            if (token.isNotEmpty()) {
                cloneCommand.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider("oauth2", token)
                )
            }
            cloneCommand.call()
            Result.success("Cloned successfully to ${targetDir.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun push(projectDir: File, token: String): Result<String> {
        if (!File(projectDir, ".git").exists()) {
            return Result.failure(Exception("Not a git repository"))
        }
        return try {
            Git.open(projectDir).use { git ->
                val pushCommand = git.push()
                if (token.isNotEmpty()) {
                    pushCommand.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider("oauth2", token)
                    )
                }
                pushCommand.call()
                Result.success("Pushed changes successfully")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pull(projectDir: File, token: String): Result<String> {
        if (!File(projectDir, ".git").exists()) {
            return Result.failure(Exception("Not a git repository"))
        }
        return try {
            Git.open(projectDir).use { git ->
                val pullCommand = git.pull()
                if (token.isNotEmpty()) {
                    pullCommand.setCredentialsProvider(
                        UsernamePasswordCredentialsProvider("oauth2", token)
                    )
                }
                val result = pullCommand.call()
                if (result.isSuccessful) {
                    Result.success("Pulled changes successfully")
                } else {
                    Result.failure(Exception("Pull failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

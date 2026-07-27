package com.example.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
)

class TerminalRunner {

    suspend fun executeCommand(commandStr: String, workingDir: File?): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val dir = if (workingDir != null && workingDir.exists()) workingDir else File(".")
                val process = ProcessBuilder("/system/bin/sh", "-c", commandStr)
                    .directory(dir)
                    .redirectErrorStream(false)
                    .start()

                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

                val stdout = StringBuilder()
                val stderr = StringBuilder()

                var line: String?
                while (stdoutReader.readLine().also { line = it } != null) {
                    stdout.append(line).append("\n")
                }
                while (stderrReader.readLine().also { line = it } != null) {
                    stderr.append(line).append("\n")
                }

                val exitCode = process.waitFor()

                CommandResult(
                    exitCode = exitCode,
                    output = stdout.toString().trimEnd(),
                    error = stderr.toString().trimEnd()
                )
            } catch (e: Exception) {
                CommandResult(
                    exitCode = -1,
                    output = "",
                    error = "Failed to execute command: ${e.localizedMessage}"
                )
            }
        }
    }
}

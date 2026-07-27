package com.example.parser

import android.net.Uri
import com.example.data.model.DiagnosticItem
import com.example.data.model.DiagnosticSeverity

object CodeAnalyzer {

    fun analyzeCode(fileName: String, fileUri: Uri, code: String): List<DiagnosticItem> {
        val diagnostics = mutableListOf<DiagnosticItem>()
        val lines = code.split("\n")

        val bracketStack = mutableListOf<Pair<Char, Int>>() // char to line number

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            val trimmed = line.trim()

            // TODO check
            if (trimmed.contains("TODO", ignoreCase = true)) {
                diagnostics.add(
                    DiagnosticItem(
                        fileName = fileName,
                        fileUri = fileUri,
                        line = lineNumber,
                        message = "Found TODO: ${trimmed.take(40)}...",
                        severity = DiagnosticSeverity.WARNING
                    )
                )
            }

            // Unused import heuristic
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {
                if (fileName.endsWith(".kt") || fileName.endsWith(".py")) {
                    diagnostics.add(
                        DiagnosticItem(
                            fileName = fileName,
                            fileUri = fileUri,
                            line = lineNumber,
                            message = "Unnecessary trailing semicolon in import",
                            severity = DiagnosticSeverity.WARNING,
                            quickFix = "Remove semicolon"
                        )
                    )
                }
            }

            // Bracket checking
            for (ch in line) {
                when (ch) {
                    '{', '(', '[' -> bracketStack.add(ch to lineNumber)
                    '}' -> {
                        if (bracketStack.isNotEmpty() && bracketStack.last().first == '{') {
                            bracketStack.removeAt(bracketStack.size - 1)
                        } else {
                            diagnostics.add(
                                DiagnosticItem(
                                    fileName = fileName,
                                    fileUri = fileUri,
                                    line = lineNumber,
                                    message = "Unmatched closing brace '}'",
                                    severity = DiagnosticSeverity.ERROR
                                )
                            )
                        }
                    }
                    ')' -> {
                        if (bracketStack.isNotEmpty() && bracketStack.last().first == '(') {
                            bracketStack.removeAt(bracketStack.size - 1)
                        } else {
                            diagnostics.add(
                                DiagnosticItem(
                                    fileName = fileName,
                                    fileUri = fileUri,
                                    line = lineNumber,
                                    message = "Unmatched closing parenthesis ')'",
                                    severity = DiagnosticSeverity.ERROR
                                )
                            )
                        }
                    }
                    ']' -> {
                        if (bracketStack.isNotEmpty() && bracketStack.last().first == '[') {
                            bracketStack.removeAt(bracketStack.size - 1)
                        } else {
                            diagnostics.add(
                                DiagnosticItem(
                                    fileName = fileName,
                                    fileUri = fileUri,
                                    line = lineNumber,
                                    message = "Unmatched closing bracket ']'",
                                    severity = DiagnosticSeverity.ERROR
                                )
                            )
                        }
                    }
                }
            }
        }

        // Unclosed brackets
        for ((bracket, lineNum) in bracketStack) {
            val expected = when (bracket) {
                '{' -> "}"
                '(' -> ")"
                '[' -> "]"
                else -> ""
            }
            diagnostics.add(
                DiagnosticItem(
                    fileName = fileName,
                    fileUri = fileUri,
                    line = lineNum,
                    message = "Unclosed bracket '$bracket', missing '$expected'",
                    severity = DiagnosticSeverity.ERROR,
                    quickFix = "Insert '$expected'"
                )
            )
        }

        return diagnostics
    }
}

package com.example.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object CodeTokenizer {

    fun highlightCode(code: String, language: String, isDark: Boolean): AnnotatedString {
        if (code.isEmpty()) return buildAnnotatedString { }

        val keywordColor = if (isDark) Color(0xFFCF8E6D) else Color(0xFF0033B3)
        val stringColor = if (isDark) Color(0xFF6AAB73) else Color(0xFF067D17)
        val commentColor = if (isDark) Color(0xFF7A7E85) else Color(0xFF8C8C8C)
        val numberColor = if (isDark) Color(0xFF2AACB8) else Color(0xFF1750EB)
        val typeColor = if (isDark) Color(0xFF56A8F5) else Color(0xFF000080)
        val annotationColor = if (isDark) Color(0xFFB3AE60) else Color(0xFF9E880D)
        val normalTextColor = if (isDark) Color(0xFFDFE1E5) else Color(0xFF1E1F22)

        val keywords = when (language) {
            "kotlin", "gradle" -> setOf(
                "package", "import", "class", "interface", "fun", "val", "var",
                "if", "else", "when", "for", "while", "return", "data", "object",
                "sealed", "private", "public", "protected", "override", "open",
                "null", "true", "false", "this", "super", "try", "catch", "finally",
                "throw", "as", "is", "in", "by", "suspend", "coroutine"
            )
            "java" -> setOf(
                "package", "import", "public", "private", "protected", "class",
                "interface", "extends", "implements", "static", "final", "void",
                "int", "double", "float", "boolean", "if", "else", "for", "while",
                "return", "new", "this", "super", "try", "catch", "null", "true", "false"
            )
            "python" -> setOf(
                "def", "class", "import", "from", "as", "if", "elif", "else",
                "for", "while", "return", "try", "except", "finally", "with",
                "lambda", "pass", "break", "continue", "True", "False", "None",
                "and", "or", "not", "is", "in"
            )
            "javascript", "typescript" -> setOf(
                "import", "export", "from", "function", "const", "let", "var",
                "class", "if", "else", "for", "while", "return", "async", "await",
                "try", "catch", "null", "undefined", "true", "false", "new", "this"
            )
            else -> setOf("if", "else", "return", "function", "class", "var", "val")
        }

        return buildAnnotatedString {
            val lines = code.split("\n")
            lines.forEachIndexed { lineIdx, line ->
                var i = 0
                val len = line.length

                while (i < len) {
                    val remaining = line.substring(i)

                    // Comments
                    if (remaining.startsWith("//") || remaining.startsWith("#")) {
                        withStyle(SpanStyle(color = commentColor)) {
                            append(remaining)
                        }
                        i = len
                        continue
                    }

                    // Strings
                    if (remaining.startsWith("\"") || remaining.startsWith("'")) {
                        val quote = remaining[0]
                        var end = 1
                        while (end < remaining.length && (remaining[end] != quote || remaining[end - 1] == '\\')) {
                            end++
                        }
                        if (end < remaining.length) end++
                        val strToken = remaining.substring(0, end)
                        withStyle(SpanStyle(color = stringColor)) {
                            append(strToken)
                        }
                        i += strToken.length
                        continue
                    }

                    // Annotations/Decorators
                    if (remaining.startsWith("@")) {
                        val match = Regex("^@[A-Za-z0-9_]+").find(remaining)
                        if (match != null) {
                            val token = match.value
                            withStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold)) {
                                append(token)
                            }
                            i += token.length
                            continue
                        }
                    }

                    // Identifiers / Keywords
                    val wordMatch = Regex("^[A-Za-z_][A-Za-z0-9_]*").find(remaining)
                    if (wordMatch != null) {
                        val word = wordMatch.value
                        when {
                            keywords.contains(word) -> {
                                withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)) {
                                    append(word)
                                }
                            }
                            word[0].isUpperCase() -> {
                                withStyle(SpanStyle(color = typeColor)) {
                                    append(word)
                                }
                            }
                            else -> {
                                withStyle(SpanStyle(color = normalTextColor)) {
                                    append(word)
                                }
                            }
                        }
                        i += word.length
                        continue
                    }

                    // Numbers
                    val numMatch = Regex("^[0-9]+(\\.[0-9]+)?").find(remaining)
                    if (numMatch != null) {
                        val numStr = numMatch.value
                        withStyle(SpanStyle(color = numberColor)) {
                            append(numStr)
                        }
                        i += numStr.length
                        continue
                    }

                    // Characters / Symbols
                    withStyle(SpanStyle(color = normalTextColor)) {
                        append(line[i].toString())
                    }
                    i++
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }
}

package com.example.parser

object SymbolIndexer {

    fun extractSymbols(code: String): List<String> {
        val symbols = mutableSetOf<String>()
        val regex = Regex("""\b(fun|val|var|class|interface|object|def|function|const|let)\s+([A-Za-z0-9_]+)""")

        regex.findAll(code).forEach { match ->
            if (match.groupValues.size >= 3) {
                symbols.add(match.groupValues[2])
            }
        }

        // Standard language keywords
        symbols.addAll(
            listOf(
                "println", "print", "toString", "length", "size", "substring",
                "filter", "map", "forEach", "contains", "isEmpty", "isNotEmpty",
                "override", "suspend", "coroutine", "Context", "String", "Int", "Boolean"
            )
        )

        return symbols.toList().sorted()
    }
}

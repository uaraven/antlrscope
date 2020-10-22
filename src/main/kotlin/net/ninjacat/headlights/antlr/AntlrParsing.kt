package net.ninjacat.headlights.antlr

import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.tool.Grammar

data class AntlrResult(
        val tree: ParseTree?,
        val tokens: List<LexerToken>?,
        val grammar: Grammar,
        val errors: List<ErrorMessage>
) {
    fun isLexer() = tokens != null
}

enum class ErrorSource {
    GRAMMAR,
    CODE,
    UNKNOWN
}

data class LexerToken(val text: String, val type: String)

data class ErrorMessage(
        val line: Int,
        val pos: Int,
        val message: String?,
        val errorSource: ErrorSource
) {
    fun getPosition() = if (line == -1 || pos == -1) "Unknown" else "${line}:${pos}"
}

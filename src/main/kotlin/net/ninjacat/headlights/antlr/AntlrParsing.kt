package net.ninjacat.headlights.antlr

import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token

enum class ErrorSource {
    GRAMMAR,
    CODE,
    GENERATED_PARSER,
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

fun convertTokens(lexer: Lexer, tokens: List<Token>): List<LexerToken> {
    val types = reverseTokenTypeMap(lexer.tokenTypeMap)
    return tokens.map {
        LexerToken(it.text, types.getValue(it.type))
    }
}

private fun reverseTokenTypeMap(tokenTypeMap: Map<String, Int>): Map<Int, String> =
    tokenTypeMap.entries.map { it.value to it.key }.toMap()

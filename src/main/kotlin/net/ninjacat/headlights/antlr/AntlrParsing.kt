package net.ninjacat.headlights.antlr

import javafx.scene.control.TreeItem
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*
import kotlin.random.Random
import kotlin.random.nextUInt

enum class ErrorSource {
    GRAMMAR,
    CODE,
    GENERATED_PARSER,
    UNKNOWN
}

enum class NodeType {
    RULE,
    TERMINAL,
    ERROR
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

abstract class ParseTreeNode(val type: NodeType, open val text: String, val children: List<ParseTreeNode>) {
    val id = UUID.randomUUID().toString()
    abstract fun repr(): String
}

class ParseTreeRule(name: String, children: List<ParseTreeNode>): ParseTreeNode(NodeType.RULE, name, children) {
    override fun repr(): String {
        return "<$text>"
    }
}
class ParseTreeTerminal(private val token: String): ParseTreeNode(NodeType.TERMINAL, token, listOf()) {
    override fun repr(): String {
        return "Token: $text"
    }

}
class ParseTreeError(val line: Int, val pos: Int, message: String): ParseTreeNode(NodeType.ERROR, message, listOf()) {
    override fun repr(): String {
        return "Error: $text"
    }
}

fun convertParseTree(node: ParseTree, ruleNames: Array<String>): ParseTreeNode {
    return when(node) {
        is ErrorNode -> ParseTreeError(node.symbol.line, node.symbol.charPositionInLine, node.text)
        is TerminalNode -> ParseTreeTerminal(node.text)
        else -> {
            val ctx = node as ParserRuleContext
            val children = sequence {
                for (i in 0 until node.childCount) {
                    yield(node.getChild(i))
                }
            }
            ParseTreeRule(ruleNames[ctx.ruleIndex],
                children.map { convertParseTree(it, ruleNames) }.toList())
        }
    }
}


fun convertTokens(lexer: Lexer, tokens: List<Token>): List<LexerToken> {
    val types = reverseTokenTypeMap(lexer.tokenTypeMap)
    return tokens.map {
        LexerToken(it.text, types.getValue(it.type))
    }
}

private fun reverseTokenTypeMap(tokenTypeMap: Map<String, Int>): Map<Int, String> =
    tokenTypeMap.entries.map { it.value to it.key }.toMap()

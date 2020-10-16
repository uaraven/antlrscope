package net.ninjacat.headlights

import org.antlr.runtime.ANTLRStringStream
import org.antlr.v4.Tool
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.tool.ANTLRMessage
import org.antlr.v4.tool.ANTLRToolListener
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST
import java.util.*

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

class ErrorListener(val errors: MutableList<ErrorMessage>) : ANTLRErrorListener {

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        errors.add(ErrorMessage(line, charPositionInLine, msg, ErrorSource.CODE))
    }

    override fun reportAmbiguity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        exact: Boolean,
        ambigAlts: BitSet?,
        configs: ATNConfigSet?
    ) {
    }

    override fun reportAttemptingFullContext(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        conflictingAlts: BitSet?,
        configs: ATNConfigSet?
    ) {
    }

    override fun reportContextSensitivity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet?
    ) {
    }

}

class ParseListener(private val errors: MutableList<ErrorMessage>) : ANTLRToolListener {
    override fun info(msg: String?) {
    }

    override fun error(msg: ANTLRMessage?) {
        errors.add(
            ErrorMessage(
                msg?.line!!, msg.charPosition,
                formatErrorArgs(msg.errorType.msg, msg.args), ErrorSource.GRAMMAR))
    }

    private fun formatErrorArgs(msg: String, args: Array<Any>): String? {
        var result = msg
        for (index in args.indices) {
            val indexStr = if (index == 0) "" else index.toString()
            result = result.replace("<arg${indexStr}>", args[index].toString())
        }
        return result
    }

    override fun warning(msg: ANTLRMessage?) {
    }

}

object AntlrGen {
    private val errors = mutableListOf<ErrorMessage>()

    private fun parseGrammar(grammar: String): Grammar {
        errors.clear()
        val antlr = Tool()
        val stream = ANTLRStringStream(grammar)
        antlr.addListener(ParseListener(errors))
        val t: GrammarRootAST = antlr.parse("<string>", stream)
        val g: Grammar = antlr.createGrammar(t)
        antlr.process(g, false)
        return g
    }

    fun generateTree(grammar: String, text: String): AntlrResult {
        val g = parseGrammar(grammar)
        try {
            val lexEngine = g.createLexerInterpreter(CharStreams.fromString(text))
            val errorListener = ErrorListener(errors)
            lexEngine.addErrorListener(errorListener)
            return if (g.isCombined) {
                val tokens = CommonTokenStream(lexEngine)
                val parser = g.createParserInterpreter(tokens)
                parser.addErrorListener(errorListener)
                AntlrResult(parser.parse(g.getRule(0).index), null, g, errorListener.errors)
            } else {
                val types = reverse(lexEngine.tokenTypeMap)
                val tokens = lexEngine.allTokens.map {
                    LexerToken(it.text, types.getValue(it.type))
                }
                AntlrResult(null, tokens, g, errors)
            }
        } catch (ex: Exception) {
            return AntlrResult(null, listOf(), g, errors)
        }

    }

    private fun reverse(tokenTypeMap: Map<String, Int>): Map<Int, String> =
        tokenTypeMap.entries.map { it.value to it.key }.toMap()

}
package net.ninjacat.headlights.antlr

import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.tool.ANTLRMessage
import org.antlr.v4.tool.ANTLRToolListener
import org.antlr.v4.tool.Grammar
import java.io.Closeable
import java.util.*

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

class ToolListener(private val errors: MutableList<ErrorMessage>) : ANTLRToolListener {
    override fun info(msg: String?) {
    }

    override fun error(msg: ANTLRMessage?) {
        errors.add(
                ErrorMessage(
                        msg?.line!!, msg.charPosition + 1,
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

abstract class AntlrGrammarParser(val grammar: String, val text: String): Closeable {
    protected val errors = mutableListOf<ErrorMessage>()
    protected var tree: ParseTreeNode? = null
    protected var tokens: List<LexerToken> = listOf()
    protected var antlrGrammar: Grammar? = null
    protected var ruleNames: Array<String> = arrayOf()

    abstract fun parse(): Boolean

    fun hasTree() = tree != null

    fun hasTokens() = tokens.isNotEmpty()

    fun antlrGrammar() = antlrGrammar

    fun errors() = errors

    fun tokens() = tokens

    fun parseTree(): ParseTreeNode? = tree

    fun ruleNames(): Array<String> = ruleNames

}

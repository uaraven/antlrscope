package net.ninjacat.headlights

import org.antlr.runtime.ANTLRStringStream
import org.antlr.v4.Tool
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST
import java.util.*

data class AntlrResult(val tree: ParseTree?, val tokens: List<LexerToken>?, val grammar: Grammar, val errors: List<ErrorMessage>) {
    fun isLexer() = tokens != null
}

data class LexerToken(val text: String, val type: String)

data class ErrorMessage(val line: Int, val pos: Int, val symbol: Any?, val message: String?, val ex: RecognitionException?) {
    fun getPosition() = "${line}:${pos}"
}

class ErrorListener: ANTLRErrorListener {
    val errors = mutableListOf<ErrorMessage>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        errors.add(ErrorMessage(line, charPositionInLine, offendingSymbol, msg, e))
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

object AntlrGen {
    private fun parseGrammar(grammar: String): Grammar {
        val antlr = Tool()
        val stream = ANTLRStringStream(grammar)
        val t: GrammarRootAST = antlr.parse("<string>", stream)
        val g: Grammar = antlr.createGrammar(t)
        antlr.process(g, false)
        return g
    }

    fun generateTree(grammar: String, text: String): AntlrResult {
        val g =  parseGrammar(grammar)
        val lexEngine = g.createLexerInterpreter(CharStreams.fromString(text))
        val errorListener = ErrorListener()
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
            AntlrResult(null, tokens, g, errorListener.errors)
        }
    }

    private fun reverse(tokenTypeMap: Map<String, Int>): Map<Int, String> =
        tokenTypeMap.entries.map { it.value to it.key }.toMap()
    
}
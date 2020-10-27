package net.ninjacat.antlrscope.antlr

import org.antlr.runtime.ANTLRStringStream
import org.antlr.v4.Tool
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ListTokenSource
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST

class AntlrInterpreter(grammar: String, text: String): AntlrGrammarParser(grammar, text) {

    override fun parse(): Boolean {
        antlrGrammar = parseGrammar(grammar)
        try {
            ruleNames = antlrGrammar!!.ruleNames
            val lexEngine = antlrGrammar!!.createLexerInterpreter(CharStreams.fromString(text))
            val lexTokens = lexEngine.allTokens
            tokens = convertTokens(lexEngine, lexTokens)
            val errorListener = ErrorListener(errors)
            lexEngine.addErrorListener(errorListener)
            if (antlrGrammar!!.isCombined) {
                val tokenStream = CommonTokenStream(ListTokenSource(lexTokens))
                val parser = antlrGrammar!!.createParserInterpreter(tokenStream)
                parser.addErrorListener(errorListener)
                tree = convertParseTree(parser.parse(antlrGrammar!!.getRule(0).index), antlrGrammar?.ruleNames!!)
                errors.addAll(errorListener.errors)
            } else {
                tree = null
            }
            return true
        } catch (ex: Exception) {
            tree = null
            tokens = listOf()
            return false
        }
    }

    override fun close() {
    }

    private fun parseGrammar(grammar: String): Grammar {
        errors.clear()
        val antlr = Tool()
        val stream = ANTLRStringStream(grammar)
        antlr.addListener(ToolListener(errors))
        val t: GrammarRootAST = antlr.parse("<string>", stream)
        val g: Grammar = antlr.createGrammar(t)
        antlr.process(g, false)
        return g
    }

}

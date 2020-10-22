package net.ninjacat.headlights.antlr

import org.antlr.runtime.ANTLRStringStream
import org.antlr.v4.Tool
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST

class AntlrInterpreter(grammar: String, text: String): AntlrGrammarParser(grammar, text) {
    private val errors = mutableListOf<ErrorMessage>()
    private var tree: ParseTree? = null
    private var tokens: List<LexerToken> = listOf()
    private var antlrGrammar: Grammar? = null

    override fun parse(): Boolean {
        antlrGrammar = parseGrammar(grammar)
        try {
            val lexEngine = antlrGrammar!!.createLexerInterpreter(CharStreams.fromString(text))
            val errorListener = ErrorListener(errors)
            lexEngine.addErrorListener(errorListener)
            if (antlrGrammar!!.isCombined) {
                val tokens = CommonTokenStream(lexEngine)
                val parser = antlrGrammar!!.createParserInterpreter(tokens)
                parser.addErrorListener(errorListener)
                tree = parser.parse(antlrGrammar!!.getRule(0).index)
                errors.addAll(errorListener.errors)
            } else {
                val types = reverse(lexEngine.tokenTypeMap)
                val tokens = lexEngine.allTokens.map {
                    LexerToken(it.text, types.getValue(it.type))
                }
                tree = null
                this.tokens = tokens
            }
            return true
        } catch (ex: Exception) {
            tree = null
            tokens = listOf()
            return false
        }
    }

    override fun hasTree() = tree != null

    override fun hasTokens() = tokens.isNotEmpty()

    override fun antlrGrammar() = antlrGrammar

    override fun errors() = errors

    override fun tokens() = tokens

    override fun parseTree(): ParseTree? = tree

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

    private fun reverse(tokenTypeMap: Map<String, Int>): Map<Int, String> =
        tokenTypeMap.entries.map { it.value to it.key }.toMap()

}
package net.ninjacat.antlrscope.antlr

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTree
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path

class ParserClassLoader(vararg url: URL) : URLClassLoader(url, getSystemClassLoader())

data class AntlrParseResult(val tokens: List<LexerToken>?, val tree: ParseTree?, val ruleNames: Array<String>)

class AntlrExecutor(
    val text: String,
    private val packageName: String,
    private val grammarName: String,
    private val classPath: Path
) {

    fun parse(errorListener: ANTLRErrorListener): AntlrParseResult {
        ParserClassLoader(classPath.toUri().toURL()).use { loader ->
            val charStream = CharStreams.fromString(text)
            val baseClassName = packageName + grammarName

            val lexer = loadLexer(loader, baseClassName).getConstructor(CharStream::class.java)
                .newInstance(charStream) as Lexer

            lexer.addErrorListener(errorListener)

            val lexerTokens = arrayListOf<Token>()
            lexerTokens.addAll(lexer.allTokens)
            val tokens = CommonTokenStream(ListTokenSource(lexerTokens))
            val parserClass = try {
                loader.loadClass(baseClassName + "Parser")
            } catch (cnf: ClassNotFoundException) {
                // no parser, probably lexer-only grammar
                return AntlrParseResult(convertTokens(lexer, lexerTokens), null, arrayOf())
            }
            val parser = parserClass.getConstructor(TokenStream::class.java).newInstance(tokens) as Parser

            parser.addErrorListener(errorListener)
            val startRuleName = getStartRuleName(parser)
            val startMethod = parserClass.getDeclaredMethod(startRuleName)

            val tree = startMethod.invoke(parser) as ParseTree
            return AntlrParseResult(convertTokens(lexer, lexerTokens), tree, parser.ruleNames)
        }
    }

    private fun loadLexer(
        loader: ParserClassLoader,
        baseClassName: String
    ): Class<*> {
        return try {
            loader.loadClass(baseClassName + "Lexer")
        } catch (cnf: ClassNotFoundException) {
            loader.loadClass(baseClassName)
        }
    }

    private fun getStartRuleName(parser: Parser): String = parser.ruleNames[0]

}

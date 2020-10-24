package net.ninjacat.headlights.antlr

import org.antlr.v4.Tool
import org.antlr.v4.tool.Grammar
import org.antlr.v4.tool.ast.GrammarRootAST
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

class GrammarCompilationException(msg: String) : Exception(msg);

class AntlrCompiler(grammar: String, text: String, private val javaCompiler: JavaCompiler) : AntlrGrammarParser(grammar, text) {

    private val workDir = Files.createTempDirectory("highlights")

    init {
        workDir.toFile().deleteOnExit()
    }

    override fun parse(): Boolean {
        try {
            val grammarName = getGrammarName()
            val packageName = extractPackageName()

            val path = saveGrammar(grammarName)
            val antlrTool = Tool()

            val toolListener = ToolListener(errors)

            antlrTool.addListener(toolListener)
            runTool(antlrTool, path)

            if (errors.isEmpty()) {
                val compilationSuccess = javaCompiler.compile(path.parent)

                if (compilationSuccess) {
                    val errorListener = ErrorListener(errors)
                    val runner = AntlrExecutor(text, packageName, grammarName, path.parent.resolve("classes"))
                    val results = runner.parse(errorListener)
                    tokens = results.tokens!!
                    tree = results.tree
                    ruleNames = results.ruleNames

                }
            }
            return errors.isEmpty()
        } catch (ex: Exception) {
            errors.add(ErrorMessage(-1, -1, ex.message, ErrorSource.GENERATED_PARSER))
        }
        return false
    }

    override fun close() {
        workDir.toFile().deleteRecursively()
    }

    private fun runTool(antlrTool: Tool, grammarFile: Path) {
        val sortedGrammars: List<GrammarRootAST> = antlrTool.sortGrammarByTokenVocab(listOf(grammarFile.toString()))

        for (t in sortedGrammars) {
            val g: Grammar = antlrTool.createGrammar(t)
            g.fileName = t.fileName
            antlrTool.process(g, true)
        }
    }

    private fun getGrammarName(): String {
        val matcher = grammarNameExtractor.matcher(grammar)
        if (!matcher.find() || matcher.groupCount() < 1) {
            throw GrammarCompilationException("Cannot determine grammar name")
        }
        return matcher.group(1)
    }


    private fun extractPackageName(): String {
        val matcher = PACKAGE_EXTRACTOR.matcher(grammar)
        return if (matcher.find()) {
            matcher.group(1) + "."
        } else {
            ""
        }
    }

    private fun saveGrammar(grammarName: String): Path {
        val grammarFile = workDir.resolve("$grammarName.g4")
        grammarFile.toFile().writeText(grammar, StandardCharsets.UTF_8)
        return grammarFile
    }                            

    companion object {
        val grammarNameExtractor: Pattern =
            Pattern.compile("(?:(?:lexer|parser)\\s+)?grammar\\s+([a-zA-Z_]\\w*);", Pattern.DOTALL)

        val PACKAGE_EXTRACTOR: Pattern = Pattern.compile("@header\\s+\\{\\s+package\\s+([a-zA-Z_][a-zA-Z0-9_.]+);\\s+}", Pattern.DOTALL)
    }
}

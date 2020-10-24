package net.ninjacat.headlights.antlr

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import javax.tools.*

class JavaCompiler() {
    private val errors: MutableList<ErrorMessage> = mutableListOf()

    @Throws(IOException::class)
    fun compile(sourcePath: Path): Boolean {
        errors.clear()
        val compiledPath = sourcePath.resolve("classes")
        Files.createDirectories(compiledPath)
        val sourceFiles = findSourceFiles(sourcePath)

        val options: MutableList<String> = ArrayList()
        val compiler = ToolProvider.getSystemJavaCompiler()
        val ds = DiagnosticCollector<JavaFileObject>()
        var result: Boolean
        compiler.getStandardFileManager(ds, null, null).use { mgr ->
            mgr.setLocation(StandardLocation.CLASS_OUTPUT, listOf(compiledPath.toFile()))
            val sources: Iterable<JavaFileObject> = mgr.getJavaFileObjects(*sourceFiles)
            val task = compiler.getTask(null, mgr, ds, options, null, sources)
            result = task.call()
        }
        for (d in ds.diagnostics) {
            if (d.kind == Diagnostic.Kind.ERROR) {
                errors.add(
                    ErrorMessage(
                        d.lineNumber.toInt(),
                        d.columnNumber.toInt(),
                        d.getMessage(null),
                        ErrorSource.GENERATED_PARSER
                    )
                )
            }
        }
        return result
    }

    private fun findSourceFiles(sourcePath: Path): Array<Path> {
        return Files.list(sourcePath).filter { file -> file.toString().endsWith(".java") }.collect(Collectors.toList())
            .toTypedArray()
    }
}

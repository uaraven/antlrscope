package net.ninjacat.headlights.antlr

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AntlrCompilerTest {

    @Test
    internal fun name() {
        val grammarText = javaClass.getResource("/Test.g4").readText() 

        val parser = AntlrCompiler(grammarText, "one two")
        val result = parser.parse()

        assertThat(result).isTrue
    }
}
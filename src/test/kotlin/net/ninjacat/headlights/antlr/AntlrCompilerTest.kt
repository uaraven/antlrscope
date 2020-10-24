package net.ninjacat.headlights.antlr

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AntlrCompilerTest {

    @Test
    fun testParsing() {
        val grammarText = javaClass.getResource("/Test.g4").readText() 

        val parser = getAntlr(grammarText, "one two")
        val result = parser.parse()

        assertThat(result).isTrue

        assertThat(parser.tokens()).hasSize(3)
        assertThat(parser.parseTree()).isNotNull
    }

    @Test
    fun testInvalidGrammar() {
        val grammarText = """
            grammar Test;

            sta rt:
              twoWords
              EOF
              ;

            twoWords: WORD SPACE WORD;

            WORD  : [a-zA-Z0-9_]+  ;
            SPACE : ' ' | '\t' ;
        """.trimIndent()

        val parser = getAntlr(grammarText, "one two")
        val result = parser.parse()

        assertThat(result).isFalse()

        assertThat(parser.errors()).hasSize(1)
        assertThat(parser.errors()[0].errorSource).isEqualTo(ErrorSource.GRAMMAR)
    }

    @Test
    fun testInvalidSource() {
        val grammarText = javaClass.getResource("/Test.g4").readText()

        val parser = getAntlr(grammarText, "one, two")
        val result = parser.parse()

        assertThat(result).isFalse()

        assertThat(parser.errors()).hasSize(1)
        assertThat(parser.errors()[0].errorSource).isEqualTo(ErrorSource.CODE)

    }

    private fun getAntlr(grammarText: String, text: String) = AntlrCompiler(grammarText, text, JavaCompiler())
}

package net.ninjacat.headlights

import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object G4Highlighter {

    private val KEYWORDS = listOf(
        "parser",
        "lexer",
        "grammar",
        "options",
        "import",
        "tokens",
        "channels",
        "fragment",
        "mode",
    )

    private val COMMANDS = listOf(
        "skip",
        "more",
        "popMode",
        "mode\\(.*\\)",
        "pushMode\\(.*\\)",
        "type\\(.*\\)",
        "channel\\(.*\\)"
    )

    private val KEYWORD_PATTERN = "\\b(" + KEYWORDS.joinToString("|") + ")\\b"
    private const val STRING_PATTERN = "\'([^'\\\\]|\\\\.)*\'"
    private const val PAREN_PATTERN = "\\(|\\)"
    private const val BRACE_PATTERN = "\\{|\\}"
    private const val SEPARATOR_PATTERN = ";|:"
    private const val REGEX_PATTERN = "\\.|\\*|\\?|\\+|\\[|\\]"
    private const val TOKEN_PATTERN = "\\b[A-Z][\\w_]*\\b"
    private const val PARSER_RULE_PATTERN = "\\b[a-z][\\w_]*\\b"
    private val COMMAND_PATTERN = "->\\s+(" + COMMANDS.joinToString("|") + ")"
    private const val COMMENT_PATTERN =
        ("#[^\n]*" + "|" + "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/" // for whole text processing (text blocks)
                + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)"
                ) // for visible paragraph processing (line by line)


    private val PATTERN: Pattern = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                + "|(?<COMMAND>" + COMMAND_PATTERN + ")"
                + "|(?<PAREN>" + PAREN_PATTERN + ")"
                + "|(?<BRACE>" + BRACE_PATTERN + ")"
                + "|(?<REGEX>" + REGEX_PATTERN + ")"
                + "|(?<SEPARATOR>" + SEPARATOR_PATTERN + ")"
                + "|(?<STRING>" + STRING_PATTERN + ")"
                + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                + "|(?<TOKEN>" + TOKEN_PATTERN + ")"
                + "|(?<PARSERRULE>" + PARSER_RULE_PATTERN + ")"
    )

    fun computeHighlighting(text: String): StyleSpans<Collection<String>> {
        val matcher: Matcher = PATTERN.matcher(text)
        var lastKwEnd = 0
        val spansBuilder: StyleSpansBuilder<Collection<String>> = StyleSpansBuilder()
        while (matcher.find()) {
            val styleClass = when {
                (matcher.group("KEYWORD") != null) -> "keyword"
                (matcher.group("COMMAND") != null) -> "command"
                (matcher.group("PAREN") != null) -> "paren"
                (matcher.group("REGEX") != null) -> "regex"
                (matcher.group("BRACE") != null) -> "brace"
                (matcher.group("SEPARATOR") != null) -> "separator"
                (matcher.group("STRING") != null) -> "string"
                (matcher.group("COMMENT") != null) -> "comment"
                (matcher.group("TOKEN") != null) -> "token"
                (matcher.group("PARSERRULE") != null) -> "parser-rule"
                else -> ""
            }
            spansBuilder.add(listOf(), matcher.start() - lastKwEnd)
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(Collections.emptyList(), text.length - lastKwEnd)
        return spansBuilder.create()
    }
}

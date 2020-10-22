package net.ninjacat.headlights.ui

import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import net.ninjacat.headlights.G4Highlighter
import net.ninjacat.headlights.ParagraphStyler
import org.fxmisc.richtext.CodeArea
import org.fxmisc.richtext.LineNumberFactory

class GrammarTextEditorPane: SplitPane() {
    val grammar: CodeArea = createRichEditor()
    val text: CodeArea = createRichEditor()

    init {
        configureEditors()
    }

    fun setGrammar(text: String) {
        grammar.replaceText(text)
    }

    fun setText(content: String) {
        text.replaceText(content)
    }

    private fun createRichEditor(): CodeArea {
        val result = CodeArea()
        result.paragraphGraphicFactory = LineNumberFactory.get(result)
        return result
    }

    private fun setupSyntaxHighlighting(editor: CodeArea) {
        editor.visibleParagraphs.addModificationObserver(
            ParagraphStyler(editor) { text -> G4Highlighter.computeHighlighting(text) }
        )
    }

    private fun createRichCaretPositionIndicator(editor: CodeArea): Label {
        val textPosition = Label("1:1")
        textPosition.padding = Insets(2.0, 2.0, 2.0, 2.0)
        val updateTextCaretPosition = updateRichCaretPosition(editor, textPosition)
        editor.addEventHandler(MouseEvent.MOUSE_CLICKED, updateTextCaretPosition)
        editor.addEventHandler(KeyEvent.KEY_RELEASED, updateTextCaretPosition)
        editor.textProperty().addListener { _, _, _ -> updateTextCaretPosition.handle(null) }
        return textPosition
    }

    private fun updateRichCaretPosition(editor: CodeArea, label: Label): EventHandler<Event> {
        return EventHandler<Event> {
            val line = editor.currentParagraph + 1
            val pos = editor.caretColumn + 1
            label.text = "${line}:${pos}"
        }
    }

    private fun configureEditors() {
        setupSyntaxHighlighting(grammar)

        val grammarPosition = createRichCaretPositionIndicator(grammar)
        val textPosition = createRichCaretPositionIndicator(text)

        val lGrammar = Label("Grammar")
        val hbGrammar = HBox(lGrammar)
        hbGrammar.style = "-fx-background-color: #00B0D0;"
        val lText = Label("Text")
        val hbText = HBox(lText)
        hbText.style = "-fx-background-color: #00B0D0;"

        this.items.addAll(
            vboxOf(
                growing = grammar,
                hbGrammar,
                grammar,
                grammarPosition
            ), vboxOf(
                growing = text,
                hbText,
                text,
                textPosition
            )
        )
    }

    private fun vboxOf(growing: Node?, vararg children: Node): Node {
        val vbox = VBox()
        vbox.children.addAll(children)
        if (growing != null) {
            VBox.setVgrow(growing, Priority.ALWAYS)
        }
        return vbox
    }
}
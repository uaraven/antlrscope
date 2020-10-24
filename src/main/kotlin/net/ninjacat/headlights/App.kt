package net.ninjacat.headlights

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import net.ninjacat.headlights.antlr.*
import net.ninjacat.headlights.ui.GrammarTextEditorPane
import net.ninjacat.headlights.ui.OutputPane
import java.util.function.Consumer
import kotlin.system.exitProcess


class AntlrViewApp : Application() {
    private val editors = GrammarTextEditorPane()
    private val outputPane: OutputPane = OutputPane()
    private val resultPane: VBox = VBox()
    private val mainMenu = MenuBar()
    private val antlrMode = ComboBox<String>(FXCollections.observableArrayList("Interpreted", "Compiled"))

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ANTLR in the Headlights"
        primaryStage.width = 1200.0
        primaryStage.height = 800.0

        outputPane.onErrorClick = Consumer { error -> showError(error) }

        resultPane.children?.add(outputPane)
        VBox.setVgrow(outputPane, Priority.ALWAYS)

        if (parameters.named.containsKey("grammar")) {
            editors.loadGrammar(parameters.named["grammar"])
        }
        if (parameters.named.containsKey("text")) {
            editors.loadText(parameters.named["text"])
        }

        val content = SplitPane()
        content.orientation = Orientation.VERTICAL
        content.items.addAll(
            vboxOf(growing = editors, editors, createBottomBar()),
            resultPane
        )

        createMainMenu(mainMenu, primaryStage)

        val mainContainer = VBox()
        mainContainer.id = "main"
        mainContainer.children.addAll(mainMenu, content)
        VBox.setVgrow(content, Priority.ALWAYS)

        val scene = Scene(mainContainer)
        scene.stylesheets.add(javaClass.getResource("/style.css").toExternalForm())
        scene.stylesheets.add(javaClass.getResource("/g4-highlight.css").toExternalForm())

        scene.accelerators[KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)] = Runnable {
            editors.saveAll()
        }
        scene.accelerators[KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN)] = Runnable {
            parseAndApplyGrammar()
        }

        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun createMainMenu(menu: MenuBar, stage: Stage) {
        val fileMenu = Menu("_File")
        val loadGrammarMenuItem = MenuItem("Load _Grammar")
        val loadTextMenuItem = MenuItem("Load _Text")

        val saveGrammarMenuItem = MenuItem("Save Grammar")
        val saveTextMenuItem = MenuItem("Save Text")

        val saveAllMenuItem = MenuItem("_Save All")

        val exitMenuItem = MenuItem("E_xit")

        val grammarExtensions = listOf(
            FileChooser.ExtensionFilter("Antlr4 Grammar files", "*.g4"),
            FileChooser.ExtensionFilter("All files", "*.*")
        )

        loadGrammarMenuItem.onAction = EventHandler {
            val fileChooser = FileChooser()
            fileChooser.title = "Select grammar file"
            fileChooser.extensionFilters.addAll(grammarExtensions)
            val grammarFile = fileChooser.showOpenDialog(stage)
            if (grammarFile != null) {
                editors.loadGrammar(grammarFile.absolutePath)
            }
        }

        loadTextMenuItem.onAction = EventHandler {
            val fileChooser = FileChooser()
            fileChooser.title = "Select text file"
            val file = fileChooser.showOpenDialog(stage)
            if (file != null) {
                editors.loadText(file.absolutePath)
            }
        }

        saveGrammarMenuItem.onAction = EventHandler {
            val fileChooser = FileChooser()
            fileChooser.title = "Select grammar file"
            fileChooser.extensionFilters.addAll(grammarExtensions)
            val grammarFile = fileChooser.showSaveDialog(stage)
            if (grammarFile != null) {
                editors.saveGrammar(grammarFile.absolutePath)
            }
        }

        loadTextMenuItem.onAction = EventHandler {
            val fileChooser = FileChooser()
            fileChooser.title = "Select text file"
            val file = fileChooser.showSaveDialog(stage)
            if (file != null) {
                editors.saveText(file.absolutePath)
            }
        }

        saveAllMenuItem.onAction = EventHandler {
            editors.saveAll()
        }

        exitMenuItem.onAction = EventHandler { exitProcess(0); }

        fileMenu.items.addAll(
            loadGrammarMenuItem,
            loadTextMenuItem,
            SeparatorMenuItem(),
            saveGrammarMenuItem,
            saveTextMenuItem,
            SeparatorMenuItem(),
            saveAllMenuItem,
            SeparatorMenuItem(),
            exitMenuItem
        )

        menu.menus.addAll(fileMenu)
    }

    private fun vboxOf(growing: Node?, vararg children: Node): Node {
        val vbox = VBox()
        vbox.children.addAll(children)
        if (growing != null) {
            VBox.setVgrow(growing, Priority.ALWAYS)
        }
        return vbox
    }


    private fun createBottomBar(): Node {
        val bottom = HBox()
        bottom.alignment = Pos.CENTER_LEFT
        bottom.style = ""
        val parseButton = Button("Parse")
        parseButton.onAction = EventHandler {
            parseAndApplyGrammar()
        }
        bottom.padding = Insets(2.0, 2.0, 2.0, 2.0)

        antlrMode.value = "Compiled"
        val modeLabel = Label("ANTLR mode: ")

        bottom.children.addAll(parseButton, Label("   "), modeLabel, antlrMode)
        return bottom
    }

    private fun parseAndApplyGrammar() {
        try {
            if (antlrMode.value == "Compiled") {
                AntlrCompiler(editors.grammar.text ?: "", editors.text.text ?: "", JavaCompiler())
            } else {
                AntlrInterpreter(editors.grammar.text ?: "", editors.text.text ?: "")
            }.use { parser ->

                parser.parse()

                if (parser.hasTokens()) {
                    outputPane.showTokens(parser.tokens())
                } else {
                    outputPane.clearTokens()
                }
                if (parser.hasTree()) {
                    outputPane.showTree(parser.parseTree()!!, parser.ruleNames().asList())
                } else {
                    outputPane.clearTree()
                }
                if (parser.errors().isNotEmpty()) {
                    outputPane.showErrors(parser.errors())
                }
                when {
                    parser.errors().isNotEmpty() -> outputPane.selectionModel.select(2)
                    parser.hasTree() -> outputPane.selectionModel.select(1)
                    !parser.hasTree() && parser.hasTokens() -> outputPane.selectionModel.select(0)
                }

            }
        } catch (ex: Exception) {
            outputPane.showErrors(
                listOf(
                    ErrorMessage(-1, -1, ex.message, ErrorSource.UNKNOWN)
                )
            )
            ex.printStackTrace()
        }
    }

    private fun showError(err: ErrorMessage) {
        if (err.errorSource == ErrorSource.GRAMMAR || err.errorSource == ErrorSource.CODE) {
            val editor = if (err.errorSource == ErrorSource.GRAMMAR) editors.grammar else editors.text
            val line = if (err.errorSource == ErrorSource.CODE) err.line - 1 else err.line - 1
            val pos = if (err.errorSource == ErrorSource.CODE) err.pos else err.pos - 1
            editor.moveTo(line, pos)
            editor.requestFocus()
        }
    }


}

fun main(vararg args: String) {
    Application.launch(AntlrViewApp::class.java, *args)
}

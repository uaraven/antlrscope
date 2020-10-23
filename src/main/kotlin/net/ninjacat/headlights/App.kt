package net.ninjacat.headlights

import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import net.ninjacat.headlights.antlr.AntlrCompiler
import net.ninjacat.headlights.antlr.ErrorMessage
import net.ninjacat.headlights.antlr.ErrorSource
import net.ninjacat.headlights.antlr.LexerToken
import net.ninjacat.headlights.ui.GrammarTextEditorPane
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import kotlin.system.exitProcess


class AntlrViewApp : Application() {
    private val editors = GrammarTextEditorPane()
    private val resultPane: VBox = VBox()
    private val outputPane: TabPane = TabPane()
    private val tokenView: TableView<LexerToken> = TableView()
    private val parseTree: TreeView<String> = TreeView()
    private val errors: TableView<ErrorMessage> = TableView()
    private val mainMenu = MenuBar()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "ANTLR in the Headlights"
        primaryStage.width = 1200.0
        primaryStage.height = 800.0

        configureTokenView()

        outputPane.tabs?.add(Tab("Tokens", tokenView))
        outputPane.tabs?.add(Tab("Parse Tree", parseTree))
        outputPane.tabs?.add(Tab("Errors", errors))
        outputPane.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        resultPane.children?.add(outputPane)
        VBox.setVgrow(outputPane, Priority.ALWAYS)

        configureErrorsView()

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

    private fun configureTokenView() {
        val columnType = TableColumn<LexerToken, String>("Type")
        columnType.cellValueFactory = PropertyValueFactory("type")
        val columnValue = TableColumn<LexerToken, String>("Value")
        tokenView.widthProperty().addListener { _, _, newv ->
            columnValue.minWidth = newv.toDouble() - columnType.width - 5
        }
        columnValue.cellValueFactory = PropertyValueFactory("text")
        tokenView.columns.addAll(columnType, columnValue)
        tokenView.placeholder = Label("")
    }

    private fun configureErrorsView() {
        val columnPosition = TableColumn<ErrorMessage, String>("Position")
        columnPosition.cellValueFactory = PropertyValueFactory("position")
        val columnMessage = TableColumn<ErrorMessage, String>("Message")
        errors.widthProperty().addListener { _, _, newv ->
            columnMessage.minWidth = newv.toDouble() - columnPosition.width - 5
        }
        columnMessage.cellValueFactory = PropertyValueFactory("message")
        errors.columns.addAll(columnPosition, columnMessage)
        errors.placeholder = Label("")
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
        bottom.style = ""
        val parseButton = Button("Parse")
        parseButton.onAction = EventHandler {
            parseAndApplyGrammar()
        }
        bottom.padding = Insets(2.0, 2.0, 2.0, 2.0)
        bottom.children.add(parseButton)
        return bottom
    }

    private fun parseAndApplyGrammar() {
        try {
            AntlrCompiler(editors.grammar.text ?: "", editors.text.text ?: "").use { parser ->

                parser.parse()

                populateErrorList(parser.errors())

                if (parser.hasTokens()) {
                    buildTokens(parser.tokens())
                } else {
                    buildTokens(listOf())
                }
                if (parser.hasTree()) {
                    buildTree(parser.parseTree()!!, parser.ruleNames().asList())
                } else {
                    clearTree()
                }

            }
        } catch (ex: Exception) {
            populateErrorList(
                listOf(
                    ErrorMessage(-1, -1, ex.message, ErrorSource.UNKNOWN)
                )
            )
            ex.printStackTrace()
        }
    }

    private fun populateErrorList(errorList: List<ErrorMessage>) {
        errors.items.clear()
        if (errorList.isNotEmpty()) {
            errors.items.setAll(errorList)
            outputPane.selectionModel.select(1)
            if (errorList[0].errorSource != ErrorSource.UNKNOWN && errorList[0].errorSource != ErrorSource.GENERATED_PARSER) {
                val editor = if (errorList[0].errorSource == ErrorSource.GRAMMAR) editors.grammar else editors.text
                editor.moveTo(errorList[0].line - 1, errorList[0].pos - 1)
                editor.requestFocus()
            }
        } else {
            outputPane.selectionModel.select(0)
        }
    }

    private fun buildTokens(tokens: List<LexerToken>) {
        tokenView.items.clear()
        this.tokenView.items.addAll(tokens)
    }

    private fun buildSubtree(parentUi: TreeItem<String>, parentParse: ParseTree, ruleNames: List<String>) {
        for (i in 0 until parentParse.childCount) {
            val child = parentParse.getChild(i)
            val uiChild = treeItemFromParseNode(child, ruleNames)
            uiChild.isExpanded = true
            parentUi.children.add(uiChild)
            buildSubtree(uiChild, child, ruleNames)
        }
    }

    private fun clearTree() {
        parseTree.root = null
    }

    private fun buildTree(generatedTree: ParseTree, ruleNames: List<String>) {
        val root = treeItemFromParseNode(generatedTree, ruleNames)
        root.isExpanded = true
        buildSubtree(root, generatedTree, ruleNames)
        parseTree.root = root
    }

    private fun treeItemFromParseNode(
        child: ParseTree?,
        ruleNames: List<String>
    ): TreeItem<String> {
        return when (child) {
            is ErrorNode -> {
                TreeItem("Error: ${child.text}")
            }
            is TerminalNode -> {
                TreeItem("Token: ${child.text}")
            }
            else -> {
                TreeItem("<${ruleNames[(child as ParserRuleContext).ruleIndex]}>")
            }
        }
    }

}

fun main(vararg args: String) {
    Application.launch(AntlrViewApp::class.java, *args)
}
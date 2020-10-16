package net.ninjacat.headlights

import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import org.antlr.v4.runtime.InterpreterRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors


class AntlrViewApp : Application() {
    private val hack = Font.loadFont(javaClass.getResource("/Hack-Regular.ttf").toExternalForm(), 15.0)
    private val grammar: TextArea = createGrammarEditor()
    private val text: TextArea = createGrammarEditor()
    private val resultPane: VBox = VBox()
    private val outputPane: TabPane = TabPane()
    private val resultsTab: Tab = Tab("Results", Label())
    private val errors: TableView<ErrorMessage> = TableView()

    override fun start(primaryStage: Stage?) {
        primaryStage?.title = "ANTLR in the Headlights"
        primaryStage?.width = 1200.0
        primaryStage?.height = 800.0

        val editorSplit = SplitPane()

        val lGrammar = Label("Grammar")
        val hbGrammar = HBox(lGrammar)
        hbGrammar.style = "-fx-background-color: #00B0D0;"
        val lText = Label("Text")
        val hbText = HBox(lText)
        hbText.style = "-fx-background-color: #00B0D0;"

        editorSplit.items.addAll(
            vboxOf(
                growing = grammar,
                hbGrammar,
                grammar
            ), vboxOf(
                growing = text,
                hbText,
                text
            )
        )

        outputPane.tabs?.add(resultsTab)
        outputPane.tabs?.add(Tab("Errors", errors))

        resultPane.children?.add(outputPane)
        VBox.setVgrow(outputPane, Priority.ALWAYS)


        if (parameters.named.containsKey("grammar")) {
            grammar.text = loadFile(parameters.named["grammar"])
        }
        if (parameters.named.containsKey("text")) {
            text.text = loadFile(parameters.named["text"])
        }

        val columnPosition = TableColumn<ErrorMessage, String>("Position")
        columnPosition.cellValueFactory = PropertyValueFactory("position")
        val columnMessage = TableColumn<ErrorMessage, String>("Message")
        errors.widthProperty().addListener { _, _, newv ->
            columnMessage.minWidth = newv.toDouble() - columnPosition.width - 5
        }
        columnMessage.cellValueFactory = PropertyValueFactory("message")
        errors.columns.addAll(columnPosition, columnMessage)


        val mainContainer = SplitPane()
        mainContainer.orientation = Orientation.VERTICAL
        mainContainer.items.addAll(
            vboxOf(growing = editorSplit, editorSplit, createBottomBar()),
            resultPane
        )
        mainContainer.style = "-fx-font-smoothing-type: lcd; -fx-font-size: 15"

        primaryStage?.scene = Scene(mainContainer)
        primaryStage?.show()
    }

    private fun vboxOf(growing: Node?, vararg children: Node): Node {
        val vbox = VBox()
        vbox.children.addAll(children)
        if (growing != null) {
            VBox.setVgrow(growing, Priority.ALWAYS)
        }
        return vbox
    }

    private fun loadFile(s: String?): String {
        if (s == null) {
            return ""
        }
        return Files.lines(Paths.get(s)).collect(Collectors.joining("\n"))
    }


    private fun createBottomBar(): Node {
        val bottom = HBox()
        bottom.style = ""
        val parseButton = Button("Parse")
        parseButton.onAction = EventHandler {
            onParseClicked()
        }
        bottom.padding = Insets(2.0, 2.0, 2.0, 2.0)
        bottom.children.add(parseButton)
        return bottom
    }

    private fun createGrammarEditor(): TextArea {
        val result = TextArea()
        result.font = hack
        return result
    }

    private fun onParseClicked() {
        try {
            val antlrResult = AntlrGen.generateTree(grammar.text ?: "", text.text ?: "")

            populateErrorList(antlrResult.errors)

            if (antlrResult.isLexer()) {
                buildTokens(antlrResult.tokens!!)
            } else {
                buildTree(antlrResult.tree!!, antlrResult.grammar.ruleNames.asList())
            }
        } catch (ex: Exception) {
            val errors = TextArea()
            errors.isEditable = false
            errors.text = ex.message
            setResult(errors)
            ex.printStackTrace()
        }
    }

    private fun populateErrorList(errorList: List<ErrorMessage>) {
        errors.items.clear()
        errors.items.setAll(errorList)
    }

    private fun setResult(content: Node) {
        resultsTab.content = content
    }

    private fun buildTokens(tokens: List<LexerToken>) {
        val list = ListView<String>()
        tokens.forEach {
            list.items.add("${it.type}:\n ${it.text}")
        }
        setResult(list)
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

    private fun buildTree(generatedTree: ParseTree, ruleNames: List<String>) {
        val root = treeItemFromParseNode(generatedTree, ruleNames)
        root.isExpanded = true
        buildSubtree(root, generatedTree, ruleNames)
        val tree = TreeView<String>()
        tree.root = root
        setResult(tree)
    }

    private fun treeItemFromParseNode(
        child: ParseTree?,
        ruleNames: List<String>
    ): TreeItem<String> {
        val uiChild = if (child is ErrorNode) {
            TreeItem("Error: ${child.text}")
        } else if (child is TerminalNode) {
            TreeItem("Token: ${child.text}")
        } else {
            TreeItem("<${ruleNames[(child as InterpreterRuleContext).ruleIndex]}>")
        }
        return uiChild
    }

}

fun main(vararg args: String) {
    Application.launch(AntlrViewApp::class.java, *args)
}
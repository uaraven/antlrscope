package net.ninjacat.headlights.ui

import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import net.ninjacat.headlights.antlr.ErrorMessage
import net.ninjacat.headlights.antlr.LexerToken
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.function.Consumer

class OutputPane : TabPane() {
    private val tokenView: TableView<LexerToken> = TableView()
    private val parseTree: TreeView<String> = TreeView()
    private val errors: TableView<ErrorMessage> = TableView()

    var onErrorClick: Consumer<ErrorMessage>? = null

    init {
        tabs?.add(Tab("Tokens", tokenView))
        tabs?.add(Tab("Parse Tree", parseTree))
        tabs?.add(Tab("Errors", errors))
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

        configureErrorsView()
    }

    fun showErrors(errorList: List<ErrorMessage>) {
        errors.items.clear()
        if (errorList.isNotEmpty()) {
            errors.items.setAll(errorList)
            val err = errorList[0]
            onErrorClick?.accept(err)
        }
    }

    fun showTokens(tokens: List<LexerToken>) {
        tokenView.items.clear()
        this.tokenView.items.addAll(tokens)
    }

    fun showTree(generatedTree: ParseTree, ruleNames: List<String>) {
        val root = treeItemFromParseNode(generatedTree, ruleNames)
        root.isExpanded = true
        buildSubtree(root, generatedTree, ruleNames)
        parseTree.root = root
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

    fun clearTokens() {
        tokenView.items.clear()
    }

    fun clearTree() {
        parseTree.root = null
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

        errors.setRowFactory { tv ->
            val row = TableRow<ErrorMessage>()
            row.onMouseClicked = EventHandler { event ->
                if (!row.isEmpty && event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                    onErrorClick?.accept(row.item)
                }
            }
            row
        }
    }

}

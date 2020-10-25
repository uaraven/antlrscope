package net.ninjacat.antlrscope.ui

import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.MouseButton
import net.ninjacat.antlrscope.antlr.ErrorMessage
import net.ninjacat.antlrscope.antlr.LexerToken
import net.ninjacat.antlrscope.antlr.ParseTreeNode
import java.util.function.Consumer

class OutputPane : TabPane() {
    private val tokenView: TableView<LexerToken> = TableView()
    private val errors: TableView<ErrorMessage> = TableView()
    private val graphvizView: GraphvizView = GraphvizView()
    var onErrorClick: Consumer<ErrorMessage>? = null

    private var tree: ParseTreeNode? = null

    init {
        tabs?.add(Tab("Tokens", tokenView))
        tabs?.add(Tab("Parse Tree", graphvizView))
        tabs?.add(Tab("Errors", errors))

        tabClosingPolicy = TabClosingPolicy.UNAVAILABLE

        configureTokenView()
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

    fun showTree(generatedTree: ParseTreeNode) {
        tree = generatedTree
        graphvizView.displayTree(generatedTree)
    }

    fun getParseTree(): ParseTreeNode? = tree

    fun clearTokens() {
        tokenView.items.clear()
    }

    fun clearTree() {
        graphvizView.clear()
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
        errors.widthProperty().addListener { _, _, newWidth ->
            columnMessage.minWidth = newWidth.toDouble() - columnPosition.width - 5
        }
        columnMessage.cellValueFactory = PropertyValueFactory("message")
        errors.columns.addAll(columnPosition, columnMessage)
        errors.placeholder = Label("")

        errors.setRowFactory {
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

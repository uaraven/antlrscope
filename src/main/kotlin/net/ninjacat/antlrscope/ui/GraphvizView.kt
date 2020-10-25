package net.ninjacat.antlrscope.ui

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.parse.Parser
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import net.ninjacat.antlrscope.GraphvizGen
import net.ninjacat.antlrscope.antlr.ParseTreeNode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GraphvizView: ScrollPane() {

    init {
        id = "graphviz_view"
    }

    fun displayTree(node: ParseTreeNode) {
        val dot = GraphvizGen.generateDotFile(node)
        val graph = Parser().read(dot)
        val baos = ByteArrayOutputStream()
        Graphviz.fromGraph(graph).render(Format.PNG).toOutputStream(baos)
        val image = Image(ByteArrayInputStream(baos.toByteArray()))

        content = ImageView(image)
    }

    fun clear() {
        content = null
    }
}
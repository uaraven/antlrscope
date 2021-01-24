package net.ninjacat.antlrscope

import net.ninjacat.antlrscope.antlr.ParseTreeError
import net.ninjacat.antlrscope.antlr.ParseTreeNode
import net.ninjacat.antlrscope.antlr.ParseTreeTerminal

object GraphvizGen {

    private val displayStyles = mapOf(
        "bg" to "dimgray",
        "bg_node" to "azure",
        "edge_color" to "lightsteelblue",
        "error_text" to "red",
        "error_stroke" to "orangered",
        "error_shape" to "note",
        "error_style" to "filled",
        "terminal_text" to "navyblue",
        "terminal_stroke" to "blue",
        "terminal_shape" to "box",
        "terminal_style" to "filled",
        "rule_text" to "black",
        "rule_stroke" to "white",
        "rule_shape" to "box",
        "rule_style" to "\"filled,rounded\"",
    )

    private val exportStyles = mapOf(
        "bg" to "white",
        "bg_node" to "white",
        "edge_color" to "black",
        "error_text" to "red",
        "error_stroke" to "orangered",
        "error_shape" to "note",
        "error_style" to "\"\"",
        "terminal_text" to "navyblue",
        "terminal_stroke" to "blue",
        "terminal_shape" to "box",
        "terminal_style" to "\"\"",
        "rule_text" to "black",
        "rule_stroke" to "black",
        "rule_shape" to "box",
        "rule_style" to "rounded",
    )

    private var activeStyle = exportStyles

    fun generateDotFile(root: ParseTreeNode, export: Boolean = false): String {
        val nodes = mutableListOf<String>()
        val edges = mutableListOf<String>()
        activeStyle = if (export) exportStyles else displayStyles

        processNodes(root, nodes, edges)
        return "graph ParseTree {\nbgcolor=${activeStyle["bg"]}\n" + nodes.joinToString("\n") + "\n" + edges.joinToString("\n") + "\n}\n"
    }

    private fun nodeStyle(nodeType: String): String {
        return "color=${activeStyle[nodeType + "_stroke"]} style=${activeStyle[nodeType+"_style"]} " +
                "fillcolor=${activeStyle["bg_node"]} fontcolor=${activeStyle[nodeType + "_text"]} " +
                "shape=${activeStyle[nodeType + "_shape"]}"
    }

    private fun sanitize(text: String): String {
        return text.replace("\"", "").replace("'", "")
    }

    private fun escape(text: String): String {
        return text.replace("\"", "\\\"").replace("'", "\\'")
    }

    private fun processNodes(node: ParseTreeNode, nodes: MutableList<String>, edges: MutableList<String>) {
        when (node) {
            is ParseTreeError -> nodes.add("\"${sanitize(node.text)}_${node.id}\" [${nodeStyle("error")} label=\"${escape(node.text)}\"]")
            is ParseTreeTerminal -> nodes.add("\"${sanitize(node.text)}_${node.id}\" [${nodeStyle("terminal")} label=\"${escape(node.text)}\"]")
            else -> nodes.add("\"${sanitize(node.text)}_${node.id}\" [${nodeStyle("rule")} label=\"${escape(node.text)}\"]")
        }
        val edgeIds = node.children.map { "\"${sanitize(it.text)}_${it.id}\"" }.joinToString(" ")
        if (node.children.isNotEmpty()) {
            edges.add("\"${sanitize(node.text)}_${node.id}\" -- {$edgeIds} [color=${activeStyle["edge_color"]}]")
        }
        node.children.forEach { processNodes(it, nodes, edges) }
    }
}
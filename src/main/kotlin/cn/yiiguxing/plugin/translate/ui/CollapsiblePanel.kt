package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import com.intellij.icons.AllIcons
import com.intellij.ui.components.labels.LinkLabel
import net.miginfocom.layout.CC
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants.LEADING

class CollapsiblePanel(content: JComponent, expandTitle: String) {

    private val collapseButton = LinkLabel<Void>("", AllIcons.General.CollapseComponent)
    private val expandButton = LinkLabel<Void>(expandTitle, AllIcons.General.ExpandComponent).apply {
        horizontalTextPosition = LEADING
    }

    private val expandedPanel = JPanel(migLayout()).apply {
        add(content, fillX())
        val collapseButtonPanel = JPanel(migLayout()).apply {
            add(collapseButton, CC().dockNorth())
        }
        add(collapseButtonPanel, CC().dockEast())
    }
    private val collapsedPanel = JPanel(migLayout()).apply {
        add(expandButton, CC().dockEast())
    }

    private var listener: (() -> Unit)? = null

    var isCollapsed: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                onExpandOrCollapse(value)
            }
        }

    val panel = JPanel(BorderLayout()).apply {
        if (isCollapsed) add(collapsedPanel, BorderLayout.NORTH)
        else add(expandedPanel, BorderLayout.NORTH)
    }

    init {
        collapseButton.setListener({_, _ ->  isCollapsed = true}, null)
        expandButton.setListener({_, _ ->  isCollapsed = false}, null)
    }

    private fun onExpandOrCollapse(becameCollapsed: Boolean) {
        val panelToAdd: JPanel
        val panelToRemove: JPanel
        if (becameCollapsed) {
            panelToAdd = collapsedPanel
            panelToRemove = expandedPanel
        } else {
            panelToAdd = expandedPanel
            panelToRemove = collapsedPanel
        }
        panel.remove(panelToRemove)
        panel.add(panelToAdd, BorderLayout.NORTH)
        panel.validate()
        panel.repaint()

        listener?.invoke()
    }

    fun setExpandCollapseListener(listener: () -> Unit) {
        this.listener = listener
    }

}
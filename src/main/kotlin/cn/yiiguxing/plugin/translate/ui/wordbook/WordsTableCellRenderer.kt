package cn.yiiguxing.plugin.translate.ui.wordbook

import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

internal class WordsTableCellRenderer : DefaultTableCellRenderer.UIResource() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        return super.getTableCellRendererComponent(table, value, isSelected, false, row, column)
    }
}
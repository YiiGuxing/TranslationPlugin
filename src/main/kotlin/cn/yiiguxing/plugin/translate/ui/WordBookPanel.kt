package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordBookWindowForm
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.CardLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * WordBookPanel
 *
 * Created by Yii.Guxing on 2019/09/03.
 */
class WordBookPanel() : WordBookWindowForm() {

    private val tableModel: ListTableModel<WordBookItem> = ListTableModel(WordColumnInfo, ExplainsColumnInfo)

    val selectedWord: WordBookItem? get() = tableView.selectedObject

    var popupMenu: JPopupMenu? = null

    init {
        tableView.setup()
        TableSpeedSearch(tableView)
    }

    private fun TableView<WordBookItem>.setup() {
        rowSelectionAllowed = true
        setEnableAntialiasing(true)
        setModelAndUpdateColumns(tableModel)
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

        val handler = MouseHandler()
        addMouseListener(handler)
        addMouseMotionListener(handler)
    }

    fun showMessagePane() {
        (root.layout as CardLayout).show(root, CARD_MESSAGE)
    }

    fun showTable() {
        (root.layout as CardLayout).show(root, CARD_TABLE)
    }

    fun setWords(words: List<WordBookItem>) {
        tableModel.items = words
        update()
    }

    fun update() {
        tableModel.fireTableDataChanged()
    }

    private object WordsTableCellRenderer : DefaultTableCellRenderer.UIResource() {
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

    private object WordColumnInfo :
        ColumnInfo<WordBookItem, String>(message("wordbook.window.table.title.word")) {
        override fun valueOf(item: WordBookItem): String = item.word

        override fun getRenderer(item: WordBookItem?): TableCellRenderer = WordsTableCellRenderer
    }

    private object ExplainsColumnInfo :
        ColumnInfo<WordBookItem, String>(message("wordbook.window.table.title.explanation")) {
        override fun valueOf(item: WordBookItem): String = item.explains?.replace('\n', ' ') ?: ""

        override fun getRenderer(item: WordBookItem?): TableCellRenderer = WordsTableCellRenderer
    }

    private inner class MouseHandler : MouseAdapter() {

        override fun mousePressed(e: MouseEvent) {
            val p = e.point
            val table = tableView
            val row = table.rowAtPoint(p)
            val column = table.columnAtPoint(p)
            if (column == -1 || row == -1) {
                table.clearSelection()
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            val p = e.point
            val table = tableView
            val row = table.rowAtPoint(p)
            val column = table.columnAtPoint(p)
            if (column == -1 || row == -1) {
                table.clearSelection()
            } else if (SwingUtilities.isRightMouseButton(e)) {
                table.changeSelection(row, column, false, true)
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            val table = tableView
            if (SwingUtilities.isRightMouseButton(e) && !e.isConsumed && table.selectedObject != null) {
                popupMenu?.let { popup ->
                    popup.show(table, e.x, e.y)
                    e.consume()
                }
            }
        }
    }

    companion object {
        private const val CARD_MESSAGE = "CARD_MESSAGE"
        private const val CARD_TABLE = "CARD_TABLE"
    }
}
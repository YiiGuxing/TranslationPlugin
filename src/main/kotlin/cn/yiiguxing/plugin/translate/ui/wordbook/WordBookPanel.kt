package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordBookWindowForm
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.ListTableModel
import java.awt.CardLayout
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JComponent
import javax.swing.JPopupMenu
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * WordBookPanel
 */
class WordBookPanel : WordBookWindowForm() {

    private val tableModel: ListTableModel<WordBookItem> = ListTableModel(WordColumnInfo, ExplainsColumnInfo)

    val selectedWord: WordBookItem? get() = tableView.selectedObject

    var popupMenu: JPopupMenu? = null

    private var onWordDoubleClickHandler: ((WordBookItem) -> Unit)? = null

    private var onDownloadDriverHandler: ((JComponent) -> Unit)? = null

    init {
        tableView.setup()
        TableSpeedSearch(tableView)

        downloadLinkLabel.setListener({ label, _ -> onDownloadDriverHandler?.invoke(label) }, null)
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
        fireWordsChanged()
    }

    fun selectWord(wordBookItem: WordBookItem) {
        tableView.apply {
            selection = arrayListOf(wordBookItem)
            val offsetRow = if (rowHeight > 0) parent.height / rowHeight / 2 else 0
            val visibleRow = minOf(selectedRow + offsetRow, rowCount - 1)
            val rect = getCellRect(visibleRow, 0, true)
            scrollRectToVisible(rect)
        }
    }

    fun fireWordsChanged() {
        tableModel.fireTableDataChanged()
    }

    fun onWordDoubleClicked(handler: (word: WordBookItem) -> Unit) {
        onWordDoubleClickHandler = handler
    }

    fun onDownloadDriver(handler: (JComponent) -> Unit) {
        onDownloadDriverHandler = handler
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
        private val COMPARATOR = Comparator<WordBookItem> { word1, word2 -> word1.word.compareTo(word2.word, true) }

        override fun valueOf(item: WordBookItem): String = item.word

        override fun getRenderer(item: WordBookItem?): TableCellRenderer = WordsTableCellRenderer

        override fun getComparator(): Comparator<WordBookItem> = COMPARATOR
    }

    private object ExplainsColumnInfo :
        ColumnInfo<WordBookItem, String>(message("wordbook.window.table.title.explanation")) {
        override fun valueOf(item: WordBookItem): String = item.explanation?.replace('\n', ' ') ?: ""

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
            } else if (JBSwingUtilities.isRightMouseButton(e)) {
                table.changeSelection(row, column, false, true)
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            val table = tableView
            if (JBSwingUtilities.isRightMouseButton(e) && !e.isConsumed && table.selectedObject != null) {
                popupMenu?.let { popup ->
                    popup.show(table, e.x, e.y)
                    e.consume()
                }
            }
        }

        override fun mouseClicked(e: MouseEvent) {
            if (!JBSwingUtilities.isLeftMouseButton(e) || e.isConsumed || e.clickCount != 2) {
                return
            }

            tableView.selectedObject?.let { word ->
                onWordDoubleClickHandler?.let { handler ->
                    handler(word)
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
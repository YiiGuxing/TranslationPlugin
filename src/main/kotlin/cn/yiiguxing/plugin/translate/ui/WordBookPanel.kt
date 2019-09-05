package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordBookWindowForm
import cn.yiiguxing.plugin.translate.util.WordBookService
import cn.yiiguxing.plugin.translate.util.executeOnPooledThread
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.icons.AllIcons
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.CardLayout
import java.awt.Component
import java.awt.datatransfer.StringSelection
import javax.swing.JTable
import javax.swing.event.PopupMenuEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * WordBookPanel
 *
 * Created by Yii.Guxing on 2019/09/03.
 */
class WordBookPanel() : WordBookWindowForm() {

    private val tableModel: ListTableModel<WordBookItem> = ListTableModel(WordColumnInfo, ExplainsColumnInfo)

    init {
        tableView.setup()
        TableSpeedSearch(tableView)
    }

    private fun TableView<WordBookItem>.setup() {
        rowSelectionAllowed = true
        setEnableAntialiasing(true)
        setModelAndUpdateColumns(tableModel)
        selectionModel = SingleSelectionModel()

        setupMenu()
    }

    private fun TableView<WordBookItem>.setupMenu() {
        val copy = JBMenuItem(message("wordbook.window.menu.copy"), AllIcons.Actions.Copy).apply {
            addActionListener {
                selectedObject?.let { CopyPasteManager.getInstance().setContents(StringSelection(it.word)) }
            }
        }

        val delete = JBMenuItem(message("wordbook.window.menu.delete"), AllIcons.Actions.Delete).apply {
            addActionListener {
                selectedObject?.id?.let { id -> executeOnPooledThread { WordBookService.removeWord(id) } }
            }
        }

        val menu = JBPopupMenu()
        menu.add(copy)
        menu.add(delete)
        menu.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
                val enable = selectedObject != null
                copy.isEnabled = enable
                delete.isEnabled = enable
            }
        })

        componentPopupMenu = menu
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

    companion object {
        private const val CARD_MESSAGE = "CARD_MESSAGE"
        private const val CARD_TABLE = "CARD_TABLE"
    }
}
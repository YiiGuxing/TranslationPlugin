package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ListTableModel
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

internal class WordBookTableView : TableView<WordBookItem>() {

    private val tableModel: ListTableModel<WordBookItem> = ListTableModel(WordColumnInfo(), ExplainsColumnInfo())

    val selectedWord: WordBookItem? get() = selectedObject

    val selectedWords: List<WordBookItem> get() = selectedObjects

    val isMultipleSelection: Boolean
        get() = selectionModel.let {
            it.minSelectionIndex >= 0 && it.maxSelectionIndex >= 0 && it.minSelectionIndex != it.maxSelectionIndex
        }

    private var onWordDoubleClickHandler: ((WordBookItem) -> Unit)? = null

    var popupMenu: JBPopupMenu? = null

    init {
        isStriped = true
        rowSelectionAllowed = true
        setEnableAntialiasing(true)
        setModelAndUpdateColumns(tableModel)

        MouseHandler().let {
            addMouseListener(it)
            addMouseMotionListener(it)
        }
        addKeyListener(KeyHandler())

        TableSpeedSearch(this)
    }

    fun onWordDoubleClick(handler: (WordBookItem) -> Unit) {
        onWordDoubleClickHandler = handler
    }

    fun setWords(words: List<WordBookItem>) {
        tableModel.items = words
        tableModel.fireTableDataChanged()
    }

    fun selectWord(wordBookItem: WordBookItem) {
        selection = arrayListOf(wordBookItem)
        val offsetRow = if (rowHeight > 0) parent.height / rowHeight / 2 else 0
        val visibleRow = minOf(selectedRow + offsetRow, rowCount - 1)
        val rect = getCellRect(visibleRow, 0, true)
        scrollRectToVisible(rect)
    }

    private inner class KeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_ESCAPE) {
                clearSelection()
                event.consume()
            }
        }
    }

    private inner class MouseHandler : MouseAdapter() {

        override fun mouseReleased(e: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e) && !e.isConsumed && selectedObject != null) {
                popupMenu?.show(this@WordBookTableView, e.x, e.y)
                e.consume()
            }
        }

        override fun mouseClicked(e: MouseEvent) {
            if (!SwingUtilities.isLeftMouseButton(e) || e.isConsumed || e.clickCount != 2) {
                return
            }

            selectedObject?.let { word ->
                onWordDoubleClickHandler?.let { handler ->
                    handler(word)
                    e.consume()
                }
            }
        }
    }

}
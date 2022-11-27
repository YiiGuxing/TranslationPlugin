package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.util.ui.ColumnInfo
import javax.swing.table.TableCellRenderer

internal class ExplainsColumnInfo :
    ColumnInfo<WordBookItem, String>(message("wordbook.window.table.title.explanation")) {

    private val renderer = WordsTableCellRenderer()

    override fun valueOf(item: WordBookItem): String = item.explanation?.replace('\n', ' ') ?: ""

    override fun getRenderer(item: WordBookItem?): TableCellRenderer = renderer
}
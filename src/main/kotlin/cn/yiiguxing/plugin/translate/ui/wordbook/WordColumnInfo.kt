package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.util.ui.ColumnInfo
import javax.swing.table.TableCellRenderer

internal class WordColumnInfo : ColumnInfo<WordBookItem, String>(message("wordbook.window.table.title.word")) {

    private val renderer = WordsTableCellRenderer()

    override fun valueOf(item: WordBookItem): String = item.word

    override fun getRenderer(item: WordBookItem?): TableCellRenderer = renderer

    override fun getComparator(): java.util.Comparator<WordBookItem> = COMPARATOR

    companion object {
        private val COMPARATOR = Comparator<WordBookItem> { word1, word2 ->
            word1.word.compareTo(word2.word, true)
        }
    }
}
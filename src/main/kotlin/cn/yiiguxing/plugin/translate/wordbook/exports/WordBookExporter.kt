package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import java.io.OutputStream

interface WordBookExporter {

    val name: String

    val extension: String

    val availableForImport: Boolean get() = false

    fun export(words: List<WordBookItem>, outputStream: OutputStream)

}
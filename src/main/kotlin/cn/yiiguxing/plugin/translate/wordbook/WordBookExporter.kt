package cn.yiiguxing.plugin.translate.wordbook

import java.io.OutputStream

interface WordBookExporter {

    val name: String

    val extension: String

    fun export(words: List<WordBookItem>, outputStream: OutputStream)

}
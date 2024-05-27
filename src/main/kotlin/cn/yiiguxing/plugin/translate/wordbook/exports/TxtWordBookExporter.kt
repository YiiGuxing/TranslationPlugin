package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import java.io.OutputStream
import java.io.OutputStreamWriter

class TxtWordBookExporter : WordBookExporter {

    override val name: String = "TXT"

    override val extension: String = "txt"

    override fun export(words: List<WordBookItem>, outputStream: OutputStream) {
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            words.forEach { item ->
                writer.write(item.word)
                writer.write(SEPARATOR)
                item.phonetic?.let { writer.write(it) }
                writer.write(SEPARATOR)
                item.explanation?.let { writer.write(it.replace(EXPLANATION_REPLACE_REGEX, " ")) }
                writer.write(ITEM_SEPARATOR)
            }
            writer.flush()
        }
    }

    companion object {
        private const val SEPARATOR = '\t'.code
        private const val ITEM_SEPARATOR = '\n'.code
        private val EXPLANATION_REPLACE_REGEX = Regex("[\n\t]")
    }
}
package cn.yiiguxing.plugin.translate.wordbook

import java.io.OutputStream
import java.io.OutputStreamWriter

class TxtWordBookExporter : WordBookExporter {

    override val name: String = "TXT"

    override val extension: String = "txt"

    override fun export(words: List<WordBookItem>, outputStream: OutputStream) {
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8.name())
        words.forEach {
            writer.write(
                it.word + "\t"
                        + (if (it.phonetic.isNullOrEmpty()) "" else it.phonetic + "\t")
                        + (it.explanation?.replace(Regex("[\n\t]"), " ")?:" ")
                        + "\n"
            )
        }
        writer.flush()
    }
}
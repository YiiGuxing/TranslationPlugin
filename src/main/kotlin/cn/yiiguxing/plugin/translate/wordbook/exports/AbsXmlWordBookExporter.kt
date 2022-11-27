package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import org.jdom.CDATA
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import java.io.OutputStream
import java.io.OutputStreamWriter

abstract class AbsXmlWordBookExporter : WordBookExporter {

    override val extension: String = "xml"

    final override fun export(words: List<WordBookItem>, outputStream: OutputStream) {
        getOutputElement(words).writeToStream(outputStream)
    }

    abstract fun getOutputElement(words: List<WordBookItem>): Element

    companion object {
        private fun Element.writeToStream(outputStream: OutputStream) {
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                val format = Format.getPrettyFormat().apply {
                    encoding = Charsets.UTF_8.name()
                    lineSeparator = "\n"
                }
                XMLOutputter(format).output(this, writer)
                writer.close()
            }
        }

        fun Element.addChildElement(name: String, content: String?, cdata: Boolean = false): Element {
            val element = Element(name)
            if (cdata) {
                element.addContent(CDATA(content))
            } else {
                element.addContent(content)
            }

            addContent(element)

            return element
        }
    }
}
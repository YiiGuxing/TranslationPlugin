package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.util.JDOMUtil
import org.jdom.CDATA
import org.jdom.Element
import java.io.OutputStream

abstract class AbsXmlWordBookExporter : WordBookExporter {

    override val extension: String = "xml"

    final override fun export(words: List<WordBookItem>, outputStream: OutputStream) {
        JDOMUtil.write(getOutputElement(words), outputStream, "\n")
    }

    abstract fun getOutputElement(words: List<WordBookItem>): Element

    companion object {
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
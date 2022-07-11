@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import org.jdom.Attribute
import org.jdom.Element

class YoudaoXmlWordBookExporter : AbsXmlWordBookExporter() {

    override val name: String = "有道XML"

    override fun getOutputElement(words: List<WordBookItem>): Element {
        val wordbook = Element("wordbook")
        wordbook.attributes.add(Attribute("target", "youdao"))
        for (word in words) {
            wordbook.addContent(word.toElement())
        }

        return wordbook
    }

    companion object {
        private fun WordBookItem.toElement(): Element {
            return Element("item").apply {
                addChildElement("word", word)
                addChildElement("trans", explanation, true)
                addChildElement("phonetic", phonetic, true)
                if (tags.isNotEmpty()) {
                    addChildElement("tags", tags.first())
                }
            }
        }
    }
}
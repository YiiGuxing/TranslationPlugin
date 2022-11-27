package cn.yiiguxing.plugin.translate.wordbook.exports

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import org.jdom.Attribute
import org.jdom.Element

class XmlWordBookExporter : AbsXmlWordBookExporter() {

    override val name: String = "XML"

    override val availableForImport: Boolean = true

    override fun getOutputElement(words: List<WordBookItem>): Element {
        val wordbook = Element("wordbook")
        for (word in words) {
            wordbook.addContent(word.toElement())
        }

        return wordbook
    }

    companion object {
        private fun WordBookItem.toElement(): Element {
            return Element("item").apply {
                id?.let { attributes.add(Attribute("id", it.toString())) }
                addChildElement("word", word)
                addChildElement("sourceLanguage", sourceLanguage.code)
                addChildElement("targetLanguage", targetLanguage.code)
                addChildElement("phonetic", phonetic, true)
                addChildElement("explanation", explanation, true)
                addChildElement("tags", tags.joinToString(",").takeIf { it.isNotEmpty() })
                addChildElement("createdAt", createdAt.time.toString())
            }
        }
    }
}
package cn.yiiguxing.plugin.translate.wordbook.imports

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.util.JDOMUtil
import org.jdom.Element
import java.io.InputStream
import java.util.*

class XmlWordBookImporter : WordBookImporter {
    override fun InputStream.readWords(): List<WordBookItem> {
        val wordBookElement = JDOMUtil.load(this)

        check(wordBookElement.name == "wordbook" && wordBookElement.getAttributeValue("target").isNullOrEmpty()) {
            "Invalid word book"
        }

        return wordBookElement
            ?.getChildren("item")
            ?.mapNotNull { it.toWordBookItem() }
            ?: emptyList()
    }

    companion object {
        private fun Element.toWordBookItem(): WordBookItem? {
            val id = getAttributeValue("id")?.toLongOrNull()
            val word = getChildText("word").takeUnless { it.isNullOrBlank() } ?: return null
            val sourceLanguage = getLanguage("sourceLanguage") ?: return null
            val targetLanguage = getLanguage("targetLanguage") ?: return null
            val phonetic = getChildText("phonetic")
            val explanation = getChildText("explanation")
            val tags = getChildText("tags")
            val createdAt = getChildText("createdAt")?.toLongOrNull() ?: return null

            return WordBookItem(
                id,
                word,
                sourceLanguage,
                targetLanguage,
                phonetic,
                explanation,
                tags,
                Date(createdAt)
            )
        }

        private fun Element.getLanguage(name: String): Lang? = try {
            getChildText(name)?.let { Lang[it] }
        } catch (e: Exception) {
            null
        }
    }
}
package cn.yiiguxing.plugin.translate.trans.google

import cn.yiiguxing.plugin.translate.trans.text.DictionaryDocument
import cn.yiiguxing.plugin.translate.trans.text.DictionaryEntry
import cn.yiiguxing.plugin.translate.trans.text.DictionaryGroup
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument

/**
 * Google Dictionary Document Factory
 */
object GoogleDictionaryDocumentFactory : TranslationDocument.Factory<GoogleTranslation, DictionaryDocument> {
    override fun getDocument(input: GoogleTranslation): DictionaryDocument? {
        val dictionaries = input.dict?.map { gDict ->
            val entries = gDict.entry?.map {
                DictionaryEntry(it.word, it.reverseTranslation ?: emptyList(), it.score)
            } ?: emptyList()
            DictionaryGroup(gDict.pos, entries)
        }?.takeIf { it.isNotEmpty() } ?: return null

        return DictionaryDocument(dictionaries)
    }
}

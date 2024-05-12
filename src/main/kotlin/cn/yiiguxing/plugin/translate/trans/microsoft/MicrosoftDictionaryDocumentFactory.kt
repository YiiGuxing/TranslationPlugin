package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.trans.microsoft.models.DictionaryLookup
import cn.yiiguxing.plugin.translate.trans.text.DictionaryDocument
import cn.yiiguxing.plugin.translate.trans.text.DictionaryEntry
import cn.yiiguxing.plugin.translate.trans.text.DictionaryGroup
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument

/**
 * Microsoft dictionary document factory.
 */
internal object MicrosoftDictionaryDocumentFactory : TranslationDocument.Factory<DictionaryLookup, DictionaryDocument> {
    override fun getDocument(input: DictionaryLookup): DictionaryDocument? {
        if (input.translations.isEmpty()) {
            return null
        }

        val groups = input.translations
            .groupBy { it.posTag }
            .map { (posTag, translations) ->
                val entries = translations.asSequence()
                    .map { translation ->
                        DictionaryEntry(
                            translation.displayTarget,
                            translation.backTranslations.map { it.displayText },
                            translation.confidence
                        )
                    }
                    .toList()
                DictionaryGroup(posTag.displayTag, entries)
            }

        return DictionaryDocument(groups)
    }
}
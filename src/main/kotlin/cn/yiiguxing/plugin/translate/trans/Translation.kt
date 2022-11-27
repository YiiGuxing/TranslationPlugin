@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate.trans

import cn.yiiguxing.plugin.translate.trans.text.NamedTranslationDocument
import cn.yiiguxing.plugin.translate.trans.text.TranslationDocument
import cn.yiiguxing.plugin.translate.util.ObservableValue

interface TranslationAdapter {
    fun toTranslation(): Translation
}

/**
 * Translation
 */
data class Translation(
    override val original: String,
    override val translation: String?,
    override val srcLang: Lang,
    override val targetLang: Lang,
    private val sourceLangs: List<Lang> = emptyList(),
    val srcTransliteration: String? = null,
    val transliteration: String? = null,
    val spell: String? = null,
    val dictDocument: TranslationDocument? = null,
    val extraDocuments: List<NamedTranslationDocument> = emptyList()
) : BaseTranslation(original, srcLang, targetLang, translation) {

    val sourceLanguages: List<Lang> by lazy { sourceLangs.filter { it != Lang.UNKNOWN } }

    val observableFavoriteId: ObservableValue<Long?> = ObservableValue(null)

    var favoriteId: Long? by observableFavoriteId
}
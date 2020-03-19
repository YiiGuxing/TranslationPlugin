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
    val original: String,
    override val translation: String?,
    override val srcLang: Lang,
    override val targetLang: Lang,
    val srclangs: List<Lang>,
    val srcTransliteration: String? = null,
    val transliteration: String? = null,
    val spell: String? = null,
    val dictDocument: TranslationDocument? = null,
    val extraDocument: NamedTranslationDocument? = null
) : BaseTranslation(srcLang, targetLang, translation) {
    val observableFavoriteId: ObservableValue<Long?> = ObservableValue(null)

    var favoriteId: Long? by observableFavoriteId
}
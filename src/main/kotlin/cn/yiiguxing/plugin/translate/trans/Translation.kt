package cn.yiiguxing.plugin.translate.trans

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
    val trans: String?,
    val srcLang: Lang,
    val targetLang: Lang,
    @Suppress("SpellCheckingInspection")
    val srclangs: List<Lang>,
    val srcTransliteration: String? = null,
    val transliteration: String? = null,
    val dictDocument: TranslationDocument? = null,
    val basicExplains: List<String> = emptyList(),
    val otherExplains: Map<String, String> = emptyMap()
) {
    val observableFavoriteId: ObservableValue<Long?> = ObservableValue(null)

    var favoriteId: Long? by observableFavoriteId
}
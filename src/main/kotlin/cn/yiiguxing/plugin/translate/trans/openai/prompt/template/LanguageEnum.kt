package cn.yiiguxing.plugin.translate.trans.openai.prompt.template

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isAuto
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isExplicit
import cn.yiiguxing.plugin.translate.trans.Lang.Companion.isUnknown

/**
 * Language enum class for prompt template. DO NOT use this class directly in code.
 *
 * @see cn.yiiguxing.plugin.translate.trans.Lang
 */
@Suppress("unused")
internal class LanguageEnum {
    private val languages = Lang.values().associateBy { lang -> lang.name }

    /** Returns an array containing the language enumeration constants. */
    val values: Array<out Lang> get() = Lang.values()

    /** @see Lang.Companion.default */
    val default: Lang get() = Lang.default

    /** Get the language enumeration constant by [name]. */
    operator fun get(name: String) = languages[name] ?: throw NoSuchElementException("No such language: $name")

    /**
     * Get the language enumeration constant by language [code].
     * @see Lang.code
     */
    fun fromCode(code: String): Lang {
        return Lang.Companion[code].takeIf { it != Lang.UNKNOWN || code == Lang.UNKNOWN.code }
            ?: throw IllegalArgumentException("Unknown language code: $code")
    }

    /** @see Lang.Companion.isAuto */
    fun isAuto(lang: Lang): Boolean = lang.isAuto()

    /** @see Lang.Companion.isUnknown */
    fun isUnknown(lang: Lang): Boolean = lang.isUnknown()

    /** @see Lang.Companion.isExplicit */
    fun isExplicit(lang: Lang): Boolean = lang.isExplicit()
}
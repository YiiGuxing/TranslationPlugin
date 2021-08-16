package cn.yiiguxing.plugin.translate

import java.util.*

object WebPages {

    private const val BASE_URL = "https://yiiguxing.github.io/TranslationPlugin"

    fun get(vararg path: String, locale: Locale = Locale.getDefault()): String {
        val langPath = when (locale.language) {
            Locale.CHINESE.language -> ""
            Locale.JAPANESE.language -> "/ja"
            Locale.KOREAN.language -> "/ko"
            else -> "/en"
        }
        return "$BASE_URL$langPath/${path.joinToString("/")}"
    }

    fun getStarted(locale: Locale = Locale.getDefault()): String = get("start.html", locale = locale)

    fun updates(version: String = "", locale: Locale = Locale.getDefault()): String {
        val query = if (version.isEmpty()) "" else "?v=$version"
        return get("updates.html${query}", locale = locale)
    }

    fun releaseNote(version: String, dark: Boolean = false, locale: Locale = Locale.getDefault()): String {
        return get(
            "updates",
            "v${version.replace('.', '_')}.html?editor=true&dark=$dark",
            locale = locale
        )
    }

    fun support(locale: Locale = Locale.getDefault()): String = get("support.html", locale = locale)

    fun donors(locale: Locale = Locale.getDefault()): String = get("support.html#patrons", locale = locale)

}
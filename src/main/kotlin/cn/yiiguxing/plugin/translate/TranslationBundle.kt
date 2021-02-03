package cn.yiiguxing.plugin.translate

import org.jetbrains.annotations.PropertyKey

const val BUNDLE = "messages.TranslationBundle"

object TranslationBundle : MyDynamicBundle(BUNDLE)

fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return TranslationBundle.getMessage(key, *params)
}
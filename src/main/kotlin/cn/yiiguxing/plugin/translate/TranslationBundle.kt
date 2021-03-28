package cn.yiiguxing.plugin.translate

import com.intellij.AbstractBundle
import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.*

const val BUNDLE = "messages.TranslationBundle"

object TranslationBundle : AbstractBundle(BUNDLE) {

    private val adaptedControl = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)

    private val adaptedBundle: AbstractBundle? by lazy {
        val dynamicLocale = DynamicBundle.getLocale()
        if (dynamicLocale.toLanguageTag() == Locale.ENGLISH.toLanguageTag()) {
            object : AbstractBundle(BUNDLE) {
                override fun findBundle(
                    pathToBundle: String,
                    loader: ClassLoader,
                    control: ResourceBundle.Control
                ): ResourceBundle {
                    val dynamicBundle = ResourceBundle.getBundle(pathToBundle, dynamicLocale, loader, adaptedControl)
                    return dynamicBundle ?: super.findBundle(pathToBundle, loader, control)
                }
            }
        } else null
    }

    override fun findBundle(
        pathToBundle: String,
        loader: ClassLoader,
        control: ResourceBundle.Control
    ): ResourceBundle {
        val dynamicLocale = DynamicBundle.getLocale()
        val dynamicBundle = ResourceBundle.getBundle(pathToBundle, dynamicLocale, loader, control)
        return dynamicBundle ?: super.findBundle(pathToBundle, loader, control)
    }

    fun getAdaptedMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return adaptedBundle?.getMessage(key, *params) ?: getMessage(key, *params)
    }

}

fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return TranslationBundle.getMessage(key, *params)
}

fun adaptedMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return TranslationBundle.getAdaptedMessage(key, *params)
}
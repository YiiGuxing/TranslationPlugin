package cn.yiiguxing.plugin.translate

import com.intellij.AbstractBundle
import com.intellij.DynamicBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.PropertyKey
import java.util.*

open class TranslationDynamicBundle(private val pathToBundle: String) : AbstractBundle(pathToBundle) {

    private val adaptedControl = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)

    private val adaptedBundle: AbstractBundle? by lazy {
        val dynamicLocale = dynamicLocale ?: return@lazy null
        if (dynamicLocale.toLanguageTag() == Locale.ENGLISH.toLanguageTag()) {
            object : AbstractBundle(pathToBundle) {
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
        return dynamicLocale?.let { locale ->
            val controlUsed = if (forceFollowLanguagePack) adaptedControl else control
            ResourceBundle.getBundle(pathToBundle, locale, loader, controlUsed)
        } ?: super.findBundle(pathToBundle, loader, control)
    }

    fun getAdaptedMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return adaptedBundle?.getMessage(key, *params) ?: getMessage(key, *params)
    }

    companion object {
        private val LOGGER = Logger.getInstance(TranslationDynamicBundle::class.java)

        private val forceFollowLanguagePack: Boolean by lazy {
            Registry.get("cn.yiiguxing.plugin.translate.bundle.forceFollowLanguagePack").asBoolean()
        }

        val dynamicLocale: Locale? by lazy {
            try {
                DynamicBundle.getLocale()
            } catch (e: NoSuchMethodError) {
                LOGGER.debug("NoSuchMethodError: DynamicBundle.getLocale()")
                null
            }
        }

    }

}
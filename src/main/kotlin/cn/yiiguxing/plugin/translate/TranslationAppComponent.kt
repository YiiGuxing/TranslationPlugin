package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.components.ApplicationComponent

/**
 * TranslationAppComponent
 */
class TranslationAppComponent : ApplicationComponent {

    override fun getComponentName(): String = javaClass.name

    override fun initComponent() {
        // initialize translate service.
        TranslateService.install()
    }

    override fun disposeComponent() {
        TranslationUIManager.disposeUI()
        TranslateService.uninstall()
    }
}
package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.TKK
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.components.ApplicationComponent

/**
 * TranslationAppComponent
 *
 * Created by Yii.Guxing on 2018/1/11
 */
class TranslationAppComponent : ApplicationComponent {

    override fun getComponentName(): String = javaClass.name

    override fun initComponent() {
        TKK.update()
        // initialize translate service.
        TranslateService.install()
    }

    override fun disposeComponent() {
        TranslationUIManager.disposeUI()
        TranslateService.uninstall()
    }
}
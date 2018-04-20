package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.YOUDAO_AI_URL
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * YoudaoAppKeySettingsPanel
 *
 * Created by Yii.Guxing on 2018/04/19
 */
class YoudaoAppKeySettingsPanel(settings: Settings)
    : AppKeySettingsPanel(settings.youdaoTranslateSettings, YoudaoTranslator) {

    init {
        logo.icon = logoImage
    }

    @Suppress("InvalidBundleOrProperty")
    override val name: String = message("translator.name.youdao")

    override fun getAppKeyLink(): String = YOUDAO_AI_URL

    companion object {
        private val logoImage: Icon = IconLoader.getIcon("/image/youdao_translate_logo.png")
    }
}
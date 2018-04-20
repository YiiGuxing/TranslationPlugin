package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.BAIDU_FANYI_URL
import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * BaiduAppKeySettingsPanel
 *
 * Created by Yii.Guxing on 2018/04/19
 */
class BaiduAppKeySettingsPanel(settings: Settings)
    : AppKeySettingsPanel(settings.baiduTranslateSettings, BaiduTranslator) {

    init {
        logo.icon = logoImage
    }

    @Suppress("InvalidBundleOrProperty")
    override val name: String = message("translator.name.baidu")

    override fun getAppKeyLink(): String = BAIDU_FANYI_URL

    companion object {
        private val logoImage: Icon = IconLoader.getIcon("/image/baidu_translate_logo.png")
    }
}
package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppKeySettings
import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.trans.ali.AliTranslator
import cn.yiiguxing.plugin.translate.trans.baidu.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.google.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.youdao.YoudaoTranslator
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplTranslator
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsDialog
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsPanel
import cn.yiiguxing.plugin.translate.util.Settings
import icons.TranslationIcons
import javax.swing.Icon

enum class TranslationEngine(
    val id: String,
    val translatorName: String,
    val icon: Icon,
    val contentLengthLimit: Int = -1,
    val intervalLimit: Int = 500
) {

    GOOGLE("translate.google", message("translator.name.google"), TranslationIcons.Google),
    YOUDAO("ai.youdao", message("translator.name.youdao"), TranslationIcons.Youdao, 5000),
    BAIDU("fanyi.baidu", message("translator.name.baidu"), TranslationIcons.Baidu, 10000, 1000),
    ALI("translate.ali", message("translator.name.ali"), TranslationIcons.Ali, 5000),
    DEEPL("translate.deepl", message("translator.name.deepl"), TranslationIcons.Deepl, 10000, 1000);

    var primaryLanguage: Lang
        get() {
            return when (this) {
                GOOGLE -> Settings.googleTranslateSettings.primaryLanguage
                YOUDAO -> Settings.youdaoTranslateSettings.primaryLanguage
                BAIDU -> Settings.baiduTranslateSettings.primaryLanguage
                ALI -> Settings.aliTranslateSettings.primaryLanguage
                DEEPL -> Settings.deeplTranslateSettings.primaryLanguage
            }
        }
        set(value) {
            when (this) {
                GOOGLE -> Settings.googleTranslateSettings.primaryLanguage = value
                YOUDAO -> Settings.youdaoTranslateSettings.primaryLanguage = value
                BAIDU -> Settings.baiduTranslateSettings.primaryLanguage = value
                ALI -> Settings.aliTranslateSettings.primaryLanguage = value
                DEEPL -> Settings.deeplTranslateSettings.primaryLanguage = value
            }
        }

    val translator: Translator
        get() {
            return when (this) {
                GOOGLE -> GoogleTranslator
                YOUDAO -> YoudaoTranslator
                BAIDU -> BaiduTranslator
                ALI -> AliTranslator
                DEEPL -> DeeplTranslator
            }
        }

    fun supportedTargetLanguages(): List<Lang> = translator.supportedTargetLanguages

    fun isConfigured(): Boolean {
        return when (this) {
            GOOGLE -> true
            YOUDAO -> isConfigured(Settings.youdaoTranslateSettings)
            BAIDU -> isConfigured(Settings.baiduTranslateSettings)
            ALI -> isConfigured(Settings.aliTranslateSettings)
            DEEPL -> isConfigured(Settings.deeplTranslateSettings)
        }
    }

    private fun isConfigured(settings: AppKeySettings) =
        settings.appId.isNotEmpty() && settings.getAppKey().isNotEmpty()

    fun showConfigurationDialog(): Boolean {
        return when (this) {
            YOUDAO -> AppKeySettingsDialog(
                message("settings.youdao.title"),
                AppKeySettingsPanel(
                    TranslationIcons.load("/image/youdao_translate_logo.png"),
                    "https://ai.youdao.com",
                    Settings.youdaoTranslateSettings
                ),
                HelpTopic.YOUDAO
            ).showAndGet()
            BAIDU -> AppKeySettingsDialog(
                message("settings.baidu.title"),
                AppKeySettingsPanel(
                    TranslationIcons.load("/image/baidu_translate_logo.png"),
                    "https://fanyi-api.baidu.com/manage/developer",
                    Settings.baiduTranslateSettings
                ),
                HelpTopic.BAIDU
            ).showAndGet()
            ALI -> AppKeySettingsDialog(
                message("settings.ali.title"),
                AppKeySettingsPanel(
                    TranslationIcons.load("/image/ali_translate_logo.png"),
                    "https://usercenter.console.aliyun.com/#/manage/ak",
                    Settings.aliTranslateSettings
                ),
                HelpTopic.ALI
            ).showAndGet()
            DEEPL -> AppKeySettingsDialog(
                message("settings.deepl.title"),
                AppKeySettingsPanel(
                    TranslationIcons.load("/image/deepl_translate_logo.png"),
                    "https://www.deepl.com/pro-api?cta=header-pro-api",
                    Settings.deeplTranslateSettings
                ),
                HelpTopic.DEEPL
            ).showAndGet()
            else -> true
        }
    }

}

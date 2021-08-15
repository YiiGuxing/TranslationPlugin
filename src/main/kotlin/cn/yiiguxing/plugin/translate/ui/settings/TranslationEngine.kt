package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsDialog
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsPanel
import cn.yiiguxing.plugin.translate.util.Settings
import icons.Icons
import javax.swing.Icon

enum class TranslationEngine(
    val id: String,
    val translatorName: String,
    val icon: Icon,
    val contentLengthLimit: Int = 0,
    val intervalLimit: Int = 200
) {

    GOOGLE("translate.google", message("translator.name.google"), Icons.Google),
    YOUDAO("ai.youdao", message("translator.name.youdao"), Icons.Youdao, 5000),
    BAIDU("fanyi.baidu", message("translator.name.baidu"), Icons.Baidu, 10000, 1000),
    ALI("translate.ali", message("translator.name.ali"), Icons.Ali, 5000);

    var primaryLanguage: Lang
        get() {
            return when (this) {
                GOOGLE -> Settings.googleTranslateSettings.primaryLanguage
                YOUDAO -> Settings.youdaoTranslateSettings.primaryLanguage
                BAIDU -> Settings.baiduTranslateSettings.primaryLanguage
                ALI -> Settings.aliTranslateSettings.primaryLanguage
            }
        }
        set(value) {
            when (this) {
                GOOGLE -> Settings.googleTranslateSettings.primaryLanguage = value
                YOUDAO -> Settings.youdaoTranslateSettings.primaryLanguage = value
                BAIDU -> Settings.baiduTranslateSettings.primaryLanguage = value
                ALI -> Settings.aliTranslateSettings.primaryLanguage = value
            }
        }

    val translator: Translator
        get() {
            return when (this) {
                GOOGLE -> GoogleTranslator
                YOUDAO -> YoudaoTranslator
                BAIDU -> BaiduTranslator
                ALI -> AliTranslator
            }
        }

    fun supportedTargetLanguages(): List<Lang> = translator.supportedTargetLanguages

    fun isConfigured(): Boolean {
        return when (this) {
            GOOGLE -> true
            YOUDAO -> isConfigured(Settings.youdaoTranslateSettings)
            BAIDU -> isConfigured(Settings.baiduTranslateSettings)
            ALI -> isConfigured(Settings.aliTranslateSettings)
        }
    }

    private fun isConfigured(settings: AppKeySettings) =
        settings.appId.isNotEmpty() && settings.getAppKey().isNotEmpty()

    fun showConfigurationDialog(): Boolean {
        return when (this) {
            YOUDAO -> AppKeySettingsDialog(
                message("settings.youdao.title"),
                AppKeySettingsPanel(
                    Icons.load("/image/youdao_translate_logo.png"),
                    YOUDAO_AI_URL,
                    Settings.youdaoTranslateSettings
                )
            ).showAndGet()
            BAIDU -> AppKeySettingsDialog(
                message("settings.baidu.title"),
                AppKeySettingsPanel(
                    Icons.load("/image/baidu_translate_logo.png"),
                    BAIDU_FANYI_URL,
                    Settings.baiduTranslateSettings
                )
            ).showAndGet()
            ALI -> AppKeySettingsDialog(
                message("settings.ali.title"),
                AppKeySettingsPanel(
                    Icons.load("/image/ali_translate_logo.png"),
                    ALI_CAPI_URL,
                    Settings.aliTranslateSettings
                )
            ).showAndGet()
            else -> true
        }
    }

}

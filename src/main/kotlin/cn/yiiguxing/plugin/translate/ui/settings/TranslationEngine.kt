package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.AppKeySettings
import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.trans.ali.AliTranslator
import cn.yiiguxing.plugin.translate.trans.baidu.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplCredential
import cn.yiiguxing.plugin.translate.trans.deepl.DeeplTranslator
import cn.yiiguxing.plugin.translate.trans.google.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.microsoft.MicrosoftTranslator
import cn.yiiguxing.plugin.translate.trans.openai.OpenAIConfigurationDialog
import cn.yiiguxing.plugin.translate.trans.openai.OpenAICredential
import cn.yiiguxing.plugin.translate.trans.openai.OpenAITranslator
import cn.yiiguxing.plugin.translate.trans.youdao.YoudaoTranslator
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsDialog
import cn.yiiguxing.plugin.translate.ui.AppKeySettingsPanel
import cn.yiiguxing.plugin.translate.ui.DeeplConfigurationDialog
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

    MICROSOFT(
        "translate.microsoft",
        message("translation.engine.microsoft.name"),
        TranslationIcons.Engines.Microsoft,
        50000
    ),
    GOOGLE("translate.google", message("translation.engine.google.name"), TranslationIcons.Engines.Google),
    YOUDAO("ai.youdao", message("translation.engine.youdao.name"), TranslationIcons.Engines.Youdao, 5000),
    BAIDU("fanyi.baidu", message("translation.engine.baidu.name"), TranslationIcons.Engines.Baidu, 10000, 1000),
    ALI("translate.ali", message("translation.engine.ali.name"), TranslationIcons.Engines.Ali, 5000),
    DEEPL("translate.deepl", message("translation.engine.deepl.name"), TranslationIcons.Engines.Deepl, 131072, 1000),
    OPEN_AI("translate.openai", message("translation.engine.openai.name"), TranslationIcons.Engines.OpenAI, 3000);

    var primaryLanguage: Lang
        get() = Settings.primaryLanguage?.takeIf { it in supportedTargetLanguages() } ?: translator.defaultLangForLocale
        set(value) {
            Settings.primaryLanguage = if (value in supportedTargetLanguages()) {
                value
            } else {
                translator.defaultLangForLocale
            }
        }

    val translator: Translator
        get() {
            return when (this) {
                MICROSOFT -> MicrosoftTranslator
                GOOGLE -> GoogleTranslator
                YOUDAO -> YoudaoTranslator
                BAIDU -> BaiduTranslator
                ALI -> AliTranslator
                DEEPL -> DeeplTranslator
                OPEN_AI -> OpenAITranslator
            }
        }

    val hasConfiguration: Boolean
        get() = when (this) {
            MICROSOFT, GOOGLE -> false
            else -> true
        }

    fun supportedTargetLanguages(): List<Lang> = translator.supportedTargetLanguages

    fun isConfigured(): Boolean {
        return when (this) {
            MICROSOFT, GOOGLE -> true
            YOUDAO -> isConfigured(Settings.youdaoTranslateSettings)
            BAIDU -> isConfigured(Settings.baiduTranslateSettings)
            ALI -> isConfigured(Settings.aliTranslateSettings)
            DEEPL -> DeeplCredential.isAuthKeySet
            OPEN_AI -> OpenAICredential.isApiKeySet
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

            DEEPL -> DeeplConfigurationDialog().showAndGet()
            OPEN_AI -> OpenAIConfigurationDialog().showAndGet()

            else -> true
        }
    }

}

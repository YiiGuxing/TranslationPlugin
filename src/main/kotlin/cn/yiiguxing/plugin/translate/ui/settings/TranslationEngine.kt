package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.*
import icons.Icons
import javax.swing.Icon

enum class TranslationEngine(
    val id: String,
    val translatorName: String,
    val icon: Icon,
    val contentLengthLimit: Int = 0,
    val intervalLimit: Int = 0
) {

    GOOGLE("translate.google", message("translator.name.google"), Icons.Google),
    YOUDAO("ai.youdao", message("translator.name.youdao"), Icons.Youdao, 5000),
    BAIDU("fanyi.baidu", message("translator.name.baidu"), Icons.Baidu, 10000, 1000);

    var primaryLanguage: Lang
        get() {
            return when (this) {
                GOOGLE -> Settings.instance.googleTranslateSettings.primaryLanguage
                YOUDAO -> Settings.instance.youdaoTranslateSettings.primaryLanguage
                BAIDU -> Settings.instance.baiduTranslateSettings.primaryLanguage
            }
        }
        set(value) {
            when (this) {
                GOOGLE -> Settings.instance.googleTranslateSettings.primaryLanguage = value
                YOUDAO -> Settings.instance.youdaoTranslateSettings.primaryLanguage = value
                BAIDU -> Settings.instance.baiduTranslateSettings.primaryLanguage = value
            }
        }

    val translator: Translator
        get() {
            return when (this) {
                GOOGLE -> GoogleTranslator
                YOUDAO -> YoudaoTranslator
                BAIDU -> BaiduTranslator
            }
        }

    fun supportedTargetLanguages(): List<Lang> = translator.supportedTargetLanguages

}

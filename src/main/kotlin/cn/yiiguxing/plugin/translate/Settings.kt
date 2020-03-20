package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.BaiduTranslator
import cn.yiiguxing.plugin.translate.trans.GoogleTranslator
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator
import cn.yiiguxing.plugin.translate.util.PasswordSafeDelegate
import cn.yiiguxing.plugin.translate.util.SelectionMode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import kotlin.properties.Delegates

/**
 * Settings
 */
@State(name = "Settings", storages = [(Storage("yiiguxing.translation.xml"))])
class Settings : PersistentStateComponent<Settings> {

    /**
     * 翻译API
     */
    var translator: String
            by Delegates.observable(GoogleTranslator.TRANSLATOR_ID) { _, oldValue: String, newValue: String ->
                if (oldValue != newValue) {
                    settingsChangePublisher.onTranslatorChanged(this, newValue)
                }
            }

    /**
     * 谷歌翻译选项
     */
    var googleTranslateSettings: GoogleTranslateSettings = GoogleTranslateSettings()

    /**
     * 有道翻译选项
     */
    @Suppress("SpellCheckingInspection")
    var youdaoTranslateSettings: YoudaoTranslateSettings = YoudaoTranslateSettings()

    /**
     * 百度翻译选项
     */
    var baiduTranslateSettings: BaiduTranslateSettings = BaiduTranslateSettings()

    /**
     * 是否覆盖默认字体
     */
    var isOverrideFont: Boolean by Delegates.observable(false) { _, oldValue: Boolean, newValue: Boolean ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }

    /**
     * 主要字体
     */
    var primaryFontFamily: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }

    /**
     * 音标字体
     */
    var phoneticFontFamily: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }

    /**
     * 状态栏图标
     */
    var showStatusIcon: Boolean by Delegates.observable(true) { _, oldValue: Boolean, newValue: Boolean ->
        if (oldValue != newValue) {
            settingsChangePublisher.onWindowOptionsChanged(this, WindowOption.STATUS_ICON)
        }
    }

    /**
     * 翻译时需要忽略的内容
     */
    var ignoreRegExp: String? = null

    /**
     * 分隔符
     */
    var separators: String = "_- "

    /**
     * 翻译时保留文本格式
     */
    var keepFormat: Boolean = false

    /**
     * 自动播放TTS
     */
    var autoPlayTTS: Boolean = false

    var ttsSource: TTSSource = TTSSource.ORIGINAL

    /**
     * 显示词形（有道翻译）
     */
    var showWordForms: Boolean = true

    /**
     * 启动时显示每日单词
     */
    var showWordsOnStartup: Boolean = false

    /**
     * 每日单词默认显示释义
     */
    var showExplanation: Boolean = false

    /**
     * 翻译替换结果唯一时自动替换
     */
    var autoReplace: Boolean = false

    /**
     * 翻译替换前选择目标语言
     */
    var selectTargetLanguageBeforeReplacement: Boolean = false

    /**
     * 折叠原文
     */
    var foldOriginal: Boolean = false

    /**
     * 自动取词模式
     */
    var autoSelectionMode: SelectionMode = SelectionMode.INCLUSIVE

    /**
     * 目标语言选择
     */
    var targetLanguageSelection: TargetLanguageSelection = TargetLanguageSelection.DEFAULT

    @Transient
    private val settingsChangePublisher: SettingsChangeListener =
        ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangeListener.TOPIC)

    override fun getState(): Settings = this

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {

        /**
         * Get the instance of this service.
         *
         * @return the unique [Settings] instance.
         */
        val instance: Settings
            get() = ServiceManager.getService(Settings::class.java)

    }
}

@Suppress("SpellCheckingInspection")
private const val YOUDAO_SERVICE_NAME = "YIIGUXING.TRANSLATION"

@Suppress("SpellCheckingInspection")
private const val YOUDAO_APP_KEY = "YOUDAO_APP_KEY"
private const val BAIDU_SERVICE_NAME = "YIIGUXING.TRANSLATION.BAIDU"
private const val BAIDU_APP_KEY = "BAIDU_APP_KEY"

/**
 * 谷歌翻译选项
 *
 * @property primaryLanguage 主要语言
 */
@Tag("google-translate")
data class GoogleTranslateSettings(var primaryLanguage: Lang = Lang.default, var useTranslateGoogleCom: Boolean = false)

/**
 * @property primaryLanguage    主要语言
 * @property appId              应用ID
 */
@Tag("app-key")
abstract class AppKeySettings(
    var primaryLanguage: Lang,
    var appId: String = "",
    securityName: String,
    securityKey: String
) {
    private var _appKey: String? by PasswordSafeDelegate(securityName, securityKey)
        @Transient get
        @Transient set

    /** 获取应用密钥. */
    @Transient
    fun getAppKey(): String = _appKey?.trim() ?: ""

    /** 设置应用密钥. */
    @Transient
    fun setAppKey(value: String?) {
        _appKey = if (value.isNullOrBlank()) null else value
    }
}

/**
 * 有道翻译选项
 */
@Suppress("SpellCheckingInspection")
@Tag("youdao-translate")
class YoudaoTranslateSettings : AppKeySettings(
    YoudaoTranslator.defaultLangForLocale,
    securityName = YOUDAO_SERVICE_NAME,
    securityKey = YOUDAO_APP_KEY
)

/**
 * 百度翻译选项
 */
class BaiduTranslateSettings : AppKeySettings(
    BaiduTranslator.defaultLangForLocale,
    securityName = BAIDU_SERVICE_NAME,
    securityKey = BAIDU_APP_KEY
)

enum class WindowOption { STATUS_ICON }

enum class TTSSource(val displayName: String) {
    ORIGINAL(message("settings.item.original")),
    TRANSLATION(message("settings.item.translation"))
}

@Suppress("InvalidBundleOrProperty")
enum class TargetLanguageSelection(val displayName: String) {
    DEFAULT(message("default")),
    PRIMARY_LANGUAGE(message("settings.item.primaryLanguage")),
    LAST(message("settings.item.last"))
}

interface SettingsChangeListener {

    fun onTranslatorChanged(settings: Settings, translatorId: String) {}

    fun onOverrideFontChanged(settings: Settings) {}

    fun onWindowOptionsChanged(settings: Settings, option: WindowOption) {}

    companion object {
        val TOPIC: Topic<SettingsChangeListener> =
            Topic.create("TranslationSettingsChanged", SettingsChangeListener::class.java)
    }
}

package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.GOOGLE
import cn.yiiguxing.plugin.translate.util.*
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import kotlin.properties.Delegates

/**
 * Settings
 */
@State(name = "Translation.Settings", storages = [(Storage(TranslationStorages.PREFERENCES_STORAGE_NAME))])
class Settings : PersistentStateComponent<Settings> {

    @Volatile
    @Transient
    private var isInitialized = false

    /**
     * 翻译API
     */
    var translator: TranslationEngine
            by Delegates.observable(GOOGLE) { _, oldValue: TranslationEngine, newValue: TranslationEngine ->
                if (isInitialized && oldValue != newValue) {
                    SETTINGS_CHANGE_PUBLISHER.onTranslatorChanged(this, newValue)
                }
            }

    /**
     * 主要语言
     */
    var primaryLanguage: Lang? = null

    /**
     * 有道翻译选项
     */
    var youdaoTranslateSettings: YoudaoTranslateSettings = YoudaoTranslateSettings()

    /**
     * 百度翻译选项
     */
    var baiduTranslateSettings: BaiduTranslateSettings = BaiduTranslateSettings()

    /**
     * 阿里云翻译选项
     */
    var aliTranslateSettings: AliTranslateSettings = AliTranslateSettings()

    /**
     * 主要字体
     */
    var primaryFontFamily: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (isInitialized && oldValue != newValue) {
            SETTINGS_CHANGE_PUBLISHER.onOverrideFontChanged(this)
        }
    }

    /**
     * 音标字体
     */
    var phoneticFontFamily: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (isInitialized && oldValue != newValue) {
            SETTINGS_CHANGE_PUBLISHER.onOverrideFontChanged(this)
        }
    }

    /**
     * 翻译时需要忽略的内容
     */
    var ignoreRegex: String by Delegates.observable("[\\*/#\$]") { _, oldValue: String, newValue: String ->
        if (isInitialized && oldValue != newValue) {
            ignoreRegexPattern = newValue.toIgnoreRegex()
        }
    }

    @Transient
    var ignoreRegexPattern: Regex? = ignoreRegex.toIgnoreRegex()
        private set

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
     * 打开翻译对话框时取词翻译
     */
    var takeWordWhenDialogOpens: Boolean = false

    /**
     * 目标语言选择
     */
    var targetLanguageSelection: TargetLanguageSelection = TargetLanguageSelection.DEFAULT

    var translateDocumentation: Boolean = false

    var showReplacementAction: Boolean = true

    var showActionsInContextMenuOnlyWithSelection: Boolean = true

    var primaryFontPreviewText = message("settings.font.default.preview.text")

    /**
     * 单词本存储路径
     */
    var wordbookStoragePath: String? by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (isInitialized && oldValue != newValue) {
            SETTINGS_CHANGE_PUBLISHER.onWordbookStoragePathChanged(this)
        }
    }

    override fun getState(): Settings = this

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)

        val properties: PropertiesComponent = PropertiesComponent.getInstance()
        val dataVersion = properties.getInt(DATA_VERSION_KEY, 0)

        LOG.d("===== Settings Data Version: $dataVersion =====")
        if (dataVersion < CURRENT_DATA_VERSION) {
            migrate()
            properties.setValue(DATA_VERSION_KEY, CURRENT_DATA_VERSION, 0)
        }
    }

    override fun initializeComponent() {
        isInitialized = true
    }

    companion object {

        /**
         * Get the instance of this service.
         *
         * @return the unique [Settings] instance.
         */
        val instance: Settings
            get() = ApplicationManager.getApplication().getService(Settings::class.java)


        private const val CURRENT_DATA_VERSION = 1
        private const val DATA_VERSION_KEY = "${TranslationPlugin.PLUGIN_ID}.settings.data.version"

        private val LOG = Logger.getInstance(Settings::class.java)

        private fun String.toIgnoreRegex(): Regex? = takeIf { it.isNotEmpty() }?.toRegexOrNull(RegexOption.MULTILINE)

        //region Data Migration - Will be removed on v4.0
        private fun Settings.migrate() {
            LOG.d("===== Start Migration =====")
            with(PasswordSafe.instance) {
                migratePassword(youdaoTranslateSettings, YOUDAO_SERVICE_NAME, YOUDAO_APP_KEY)
                migratePassword(baiduTranslateSettings, BAIDU_SERVICE_NAME, BAIDU_APP_KEY)
                migratePassword(aliTranslateSettings, ALI_SERVICE_NAME, ALI_APP_KEY)
            }
            LOG.d("===== Migration End =====")
        }

        private fun PasswordSafe.migratePassword(settings: AppKeySettings, securityName: String, securityKey: String) {
            val securityInfo = "securityName=$securityName, securityKey=$securityKey"
            try {
                val attributes = CredentialAttributes(securityName, securityKey)
                val password = getPassword(attributes)

                LOG.d("Migrate password: $securityInfo, hasPassword=${password != null}.")

                if (password == null) {
                    return
                }

                if (password.isNotEmpty() && !settings.isAppKeySet) {
                    settings.setAppKey(password)
                    LOG.d("Password migrated: $securityInfo.")
                }
                setPassword(attributes, null)
                LOG.d("Old password removed: $securityInfo.")
            } catch (e: Throwable) {
                LOG.w("Migration failed: $securityInfo", e)
            }
        }
        //endregion
    }
}

private const val YOUDAO_SERVICE_NAME = "YIIGUXING.TRANSLATION"
private const val YOUDAO_APP_KEY = "YOUDAO_APP_KEY"
private const val BAIDU_SERVICE_NAME = "YIIGUXING.TRANSLATION.BAIDU"
private const val BAIDU_APP_KEY = "BAIDU_APP_KEY"
private const val ALI_SERVICE_NAME = "YIIGUXING.TRANSLATION.ALI"
private const val ALI_APP_KEY = "ALI_APP_KEY"

private val SETTINGS_REPOSITORY_SERVICE = generateServiceName("Settings Repository", TranslationPlugin.PLUGIN_ID)

private val SETTINGS_CHANGE_PUBLISHER: SettingsChangeListener =
    ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangeListener.TOPIC)


@Tag("app-key")
abstract class AppKeySettings(serviceKey: String) {
    var appId: String by Delegates.observable("") { _, oldValue: String, newValue: String ->
        if (oldValue != newValue) {
            SETTINGS_CHANGE_PUBLISHER.onTranslatorConfigurationChanged()
        }
    }

    private var _appKey: String? by PasswordSafeDelegate("$SETTINGS_REPOSITORY_SERVICE.$serviceKey")
        @Transient get
        @Transient set

    /** 获取应用密钥. */
    @Transient
    fun getAppKey(): String = _appKey?.trim() ?: ""

    /** 设置应用密钥. */
    @Transient
    fun setAppKey(value: String?) {
        _appKey = if (value.isNullOrBlank()) null else value
        SETTINGS_CHANGE_PUBLISHER.onTranslatorConfigurationChanged()
    }

    val isAppKeySet: Boolean
        @Transient get() = getAppKey().isNotEmpty()
}

/**
 * 有道翻译选项
 */
@Tag("youdao-translate")
class YoudaoTranslateSettings : AppKeySettings(YOUDAO_APP_KEY)

/**
 * 百度翻译选项
 */
class BaiduTranslateSettings : AppKeySettings(BAIDU_APP_KEY)

/**
 * 阿里云翻译选项
 */
class AliTranslateSettings : AppKeySettings(ALI_APP_KEY)

enum class TTSSource(val displayName: String) {
    ORIGINAL(message("settings.item.original")),
    TRANSLATION(message("settings.item.translation"))
}

enum class TargetLanguageSelection(val displayName: String) {
    DEFAULT(message("settings.item.main.or.english")),
    PRIMARY_LANGUAGE(message("settings.item.primaryLanguage")),
    LAST(message("settings.item.last"))
}

interface SettingsChangeListener {

    fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {}

    fun onTranslatorConfigurationChanged() {}

    fun onOverrideFontChanged(settings: Settings) {}

    fun onWordbookStoragePathChanged(settings: Settings) {}

    companion object {
        val TOPIC: Topic<SettingsChangeListener> =
            Topic.create("TranslationSettingsChanged", SettingsChangeListener::class.java)
    }
}

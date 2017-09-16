package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.SelectionMode
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import kotlin.properties.Delegates

/**
 * Settings
 */
@State(name = "Settings", storages = arrayOf(Storage("yiiguxing.translation.xml")))
class Settings : PersistentStateComponent<Settings> {

    /**
     * 应用ID.
     */
    var appId = ""
    /**
     * 应用密钥.
     */
    @Suppress("DEPRECATION")
    var appPrivateKey: String
        @Transient get() = PasswordSafe.getInstance().getPassword(Settings::class.java, YOUDAO_APP_PRIVATE_KEY) ?: ""
        @Transient set(value) {
            isPrivateKeyConfigured = value.isNotEmpty()
            PasswordSafe.getInstance().setPassword(Settings::class.java, YOUDAO_APP_PRIVATE_KEY, value)
        }
    var isPrivateKeyConfigured = false

    /**
     * 输入语言
     */
    var langFrom: Lang? = null
    /**
     * 输出语言
     */
    var langTo: Lang? = null

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
    var phoneticFontFamily: String?  by Delegates.observable(null) { _, oldValue: String?, newValue: String? ->
        if (oldValue != newValue) {
            settingsChangePublisher.onOverrideFontChanged(this)
        }
    }
    /**
     * 是否关闭设置APP KEY通知
     */
    var isDisableAppKeyNotification = false
    /**
     * 自动取词模式
     */
    var autoSelectionMode: SelectionMode = SelectionMode.INCLUSIVE

    @Transient private val settingsChangePublisher: SettingsChangeListener =
            ApplicationManager.getApplication().messageBus.syncPublisher(SettingsChangeListener.TOPIC)

    override fun getState(): Settings? {
        return this
    }

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun toString(): String {
        return "Settings(" +
                "appId=$appId," +
                "isPrivateKeyConfigured=$isPrivateKeyConfigured," +
                "langFrom=$langFrom," +
                "langTo=$langTo," +
                "isDisableAppKeyNotification=$isDisableAppKeyNotification," +
                "autoSelectionMode=$autoSelectionMode," +
                "settingsChangePublisher=$settingsChangePublisher" +
                ")"
    }

    companion object {

        private const val YOUDAO_APP_PRIVATE_KEY = "YOUDAO_APP_PRIVATE_KEY"

        /**
         * Get the instance of this service.
         *
         * @return the unique [Settings] instance.
         */
        val instance: Settings
            get() = ServiceManager.getService(Settings::class.java)

    }
}

interface SettingsChangeListener {

    fun onOverrideFontChanged(settings: Settings)

    companion object {
        val TOPIC: Topic<SettingsChangeListener> = Topic.create(
                "TranslationSettingsChanged",
                SettingsChangeListener::class.java
        )
    }
}

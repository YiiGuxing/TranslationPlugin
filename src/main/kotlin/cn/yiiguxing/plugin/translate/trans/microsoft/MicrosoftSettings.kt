package cn.yiiguxing.plugin.translate.trans.microsoft

import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.util.credential.SimpleStringCredentialManager
import com.intellij.credentialStore.generateServiceName
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient

@Service
@State(name = "Translation.MicrosoftSettings", storages = [Storage(TranslationStorages.PREFERENCES_STORAGE_NAME)])
internal class MicrosoftSettings : PersistentStateComponent<MicrosoftSettings> {

    var translator: MicrosoftTranslatorType = MicrosoftTranslatorType.BING_EDGE
    var region: String = ""

    @Transient
    private val keyManager = SimpleStringCredentialManager(
        generateServiceName(TranslationPlugin.PLUGIN_ID, "Microsoft Translator Subscription Key")
    )

    @Transient
    fun getSubscriptionKey(): String = keyManager.credential.orEmpty()

    @Transient
    fun setSubscriptionKey(value: String?) {
        keyManager.credential = value
    }

    @Transient
    val isSubscriptionKeySet: Boolean
        get() = keyManager.isCredentialSet

    override fun getState(): MicrosoftSettings = this

    override fun loadState(state: MicrosoftSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): MicrosoftSettings = service()
    }
}

internal enum class MicrosoftTranslatorType(val displayName: String) {
    AZURE("Azure Translator"),
    BING_EDGE("Bing + Edge Translator")
}

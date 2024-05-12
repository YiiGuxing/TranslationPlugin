package cn.yiiguxing.plugin.translate.tts

import cn.yiiguxing.plugin.translate.trans.google.GoogleSettingsDialog
import cn.yiiguxing.plugin.translate.trans.openai.ConfigType
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiCredentials
import cn.yiiguxing.plugin.translate.trans.openai.OpenAiSettings
import cn.yiiguxing.plugin.translate.trans.openai.ui.OpenAISettingsDialog
import cn.yiiguxing.plugin.translate.tts.microsoft.EdgeTTSSettingsDialog
import com.intellij.openapi.components.service
import icons.TranslationIcons
import javax.swing.Icon

enum class TTSEngine(
    val icon: Icon,
    val ttsName: String,
    val configurable: Boolean = false
) {
    EDGE(TranslationIcons.Engines.Microsoft, "Microsoft Edge TTS", true),
    GOOGLE(TranslationIcons.Engines.Google, "Google TTS", true),
    OPENAI(TranslationIcons.Engines.OpenAI, "OpenAI TTS", true);

    fun isConfigured(): Boolean {
        return when (this) {
            EDGE,
            GOOGLE -> true

            OPENAI -> service<OpenAiSettings>().let {
                it.isConfigured(ConfigType.TTS) && OpenAiCredentials.isCredentialSet(it.provider)
            }
        }
    }

    fun showConfigurationDialog(): Boolean {
        return when (this) {
            EDGE -> {
                EdgeTTSSettingsDialog().show()
                true
            }

            GOOGLE -> {
                GoogleSettingsDialog().show()
                true
            }


            OPENAI -> OpenAISettingsDialog(ConfigType.TTS).showAndGet()
        }
    }
}
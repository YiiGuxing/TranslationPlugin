@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAi Models](https://platform.openai.com/docs/models)
 */
sealed interface OpenAiModel {
    val modelId: String
    val modelName: String
}

/**
 * See: [OpenAi GPT Models](https://platform.openai.com/docs/models)
 */
enum class OpenAiGPTModel(override val modelId: String, override val modelName: String) : OpenAiModel {
    GPT_5_4_MINI("gpt-5.4-mini", "GPT-5.4 mini"),
    GPT_5_4("gpt-5.4", "GPT-5.4"),
    GPT_5_5("gpt-5.5", "GPT-5.5");

    companion object {
        /**
         * Get the default GPT model.
         */
        fun getDefault(): OpenAiGPTModel = GPT_5_4_MINI
    }
}

/**
 * See: [OpenAi TTS Models](https://platform.openai.com/docs/models/tts)
 */
enum class OpenAiTTSModel(
    override val modelId: String,
    override val modelName: String,
    val isClassic: Boolean = false
) : OpenAiModel {
    TTS_1("tts-1", "TTS-1", isClassic = true),
    TTS_1_HD("tts-1-hd", "TTS-1 HD", isClassic = true),
    GPT_4O_MINI_TTS("gpt-4o-mini-tts", "GPT-4o mini TTS"),
    ;

    companion object {
        /**
         * Get the default TTS model.
         */
        fun getDefault(): OpenAiTTSModel = TTS_1
    }
}

fun OpenAiTTSModel.getSupportedVoices(): List<OpenAiTtsVoice> = if (isClassic) {
    OpenAiTtsVoice.getClassicVoices()
} else {
    OpenAiTtsVoice.entries
}
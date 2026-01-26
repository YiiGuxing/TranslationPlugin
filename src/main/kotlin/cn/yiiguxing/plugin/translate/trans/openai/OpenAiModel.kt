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
    GPT_5("gpt-5", "GPT-5"),
    GPT_5_MINI("gpt-5-mini", "GPT-5 mini"),
    GPT_5_NANO("gpt-5-nano", "GPT-5 nano"),
    GPT_4O("gpt-4o", "GPT-4o"),
    GPT_4O_MINI("gpt-4o-mini", "GPT-4o mini"),
    GPT_4_1("gpt-4.1", "GPT-4.1"),
    GPT_4_1_MINI("gpt-4.1-mini", "GPT-4.1 mini"),
    GPT_4_1_NANO("gpt-4.1-nano", "GPT-4.1 nano"),
    GPT_4_TURBO("gpt-4-turbo", "GPT-4 Turbo"),
    GPT_4("gpt-4", "GPT-4"),
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5 Turbo"),
    O4_MINI("o4-mini", "o4-mini"),
    O3("o3", "o3"),
    O3_MINI("o3-mini", "o3-mini"),
    O1("o1", "o1"),
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
}

fun OpenAiTTSModel.getSupportedVoices(): List<OpenAiTtsVoice> = if (isClassic) {
    OpenAiTtsVoice.getClassicVoices()
} else {
    OpenAiTtsVoice.values().toList()
}
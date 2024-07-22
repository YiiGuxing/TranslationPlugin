@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAiService Models](https://platform.openai.com/docs/models)
 */
enum class OpenAiModel(val value: String, val modelName: String) {
    GPT_4O_MINI("gpt-4o-mini", "GPT-4o mini"),
    GPT_4O("gpt-4o", "GPT-4o"),
    GPT_4_TURBO("gpt-4-turbo", "GPT-4 Turbo"),
    GPT_4("gpt-4", "GPT-4"),
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5 Turbo"),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", "GPT-3.5 Turbo 1106"),

    TTS_1("tts-1", "TTS-1"),
    TTS_1_HD("tts-1-hd", "TTS-1 HD");

    companion object {
        fun gptModels(): List<OpenAiModel> {
            return OpenAiModel.values().filter { it.value.startsWith("gpt") }.toList()
        }

        fun ttsModels(): List<OpenAiModel> {
            return OpenAiModel.values().filter { it.value.startsWith("tts") }.toList()
        }
    }
}
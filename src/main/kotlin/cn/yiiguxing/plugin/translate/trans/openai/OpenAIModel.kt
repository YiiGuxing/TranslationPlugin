@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAIService Models](https://platform.openai.com/docs/models)
 */
enum class OpenAIModel(val value: String, val modelName: String) {
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5-Turbo"),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k", "GPT-3.5-Turbo-16K"),
    GPT_3_5_TURBO_INSTRUCT("gpt-3.5-turbo-instruct", "GPT-3.5-Turbo-Instruct"),
    GPT_4("gpt-4", "GPT-4"),
    GPT_4_32K("gpt-4-32k", "GPT-4-32K");
}
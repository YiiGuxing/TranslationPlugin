@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAI Models](https://platform.openai.com/docs/models)
 */
enum class OpenAIModel(val value: String, val modelName: String) {
    GPT_3_5_TURBO("gpt-3.5-turbo", "GPT-3.5-Turbo"),
    GPT_4("gpt-4", "GPT-4"),
    GPT_4_32K("gpt-4-32k", "GPT-4-32K");
}
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAI Models](https://platform.openai.com/docs/models)
 */
enum class OpenAIModel(val value: String) {
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_4("gpt-4"),
    GPT_4_32K("gpt-4-32k");
}
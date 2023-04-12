package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletionRequest

class OpenAI(
    private val apiKey: String,
    private val model: OpenAIModel = OpenAIModel.GPT_3_5_TURBO
) {

    companion object {
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
    }

    fun chatCompletion(request: ChatCompletionRequest): ChatCompletion {
        TODO("Not yet implemented")
    }

}
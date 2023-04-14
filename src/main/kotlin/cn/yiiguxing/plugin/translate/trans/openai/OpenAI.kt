package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletionRequest
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder

object OpenAI {

    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    private fun RequestBuilder.auth() {
        val apiKey = OpenAICredential.apiKey
        tuner { it.setRequestProperty("Authorization", "Bearer $apiKey") }
    }

    @RequiresBackgroundThread
    fun chatCompletion(request: ChatCompletionRequest): ChatCompletion {
        return OpenAIHttp.post<ChatCompletion>(API_URL, request) { auth() }
    }

}
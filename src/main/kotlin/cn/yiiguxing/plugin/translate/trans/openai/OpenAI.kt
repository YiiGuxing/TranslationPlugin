package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletionRequest
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder

object OpenAI {

    const val API_URL = "https://api.openai.com/v1/chat/completions"
    private val apiEndpoint: String get() = service<OpenAISettings>().apiEndpoint ?: API_URL

    private fun RequestBuilder.auth() {
        val apiKey = OpenAICredential.apiKey
        tuner { it.setRequestProperty("Authorization", "Bearer $apiKey") }
    }

    @RequiresBackgroundThread
    fun chatCompletion(request: ChatCompletionRequest): ChatCompletion {
        return OpenAIHttp.post<ChatCompletion>(apiEndpoint, request) { auth() }
    }

}

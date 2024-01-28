package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import cn.yiiguxing.plugin.translate.trans.openai.chat.chatCompletionRequest
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder

const val DEFAULT_OPEN_AI_API_ENDPOINT = "https://api.openai.com"
const val OPEN_AI_API_PATH = "/v1/chat/completions"

private const val AZURE_OPEN_AI_API_PATH = "/openai/deployments/%s/chat/completions"

interface OpenAIService {

    @RequiresBackgroundThread
    fun chatCompletion(messages: List<ChatMessage>): ChatCompletion

    interface Options {
        val model: OpenAIModel
        val endpoint: String?
    }

    interface AzureOptions : Options {
        val apiVersion: AzureServiceVersion
    }

    companion object {
        fun get(settings: OpenAISettings): OpenAIService {
            return when (settings.provider) {
                ServiceProvider.OpenAI -> OpenAI(settings.openAi)
                ServiceProvider.Azure -> Azure(settings.azure)
            }
        }
    }
}


class OpenAI(private val options: OpenAIService.Options) : OpenAIService {
    private val apiUrl: String
        get() = (options.endpoint ?: DEFAULT_OPEN_AI_API_ENDPOINT).trimEnd('/') + OPEN_AI_API_PATH

    private fun RequestBuilder.auth() {
        val apiKey = OpenAICredentials.manager(ServiceProvider.OpenAI).credential
        tuner { it.setRequestProperty("Authorization", "Bearer $apiKey") }
    }

    override fun chatCompletion(messages: List<ChatMessage>): ChatCompletion {
        val request = chatCompletionRequest {
            model = options.model.value
            this.messages = messages
        }
        return OpenAIHttp.post<ChatCompletion>(apiUrl, request) { auth() }
    }
}

class Azure(options: OpenAIService.AzureOptions) : OpenAIService {

    private val apiUrl: String = requireNotNull(options.endpoint) { "Azure OpenAI API endpoint is required" } +
            AZURE_OPEN_AI_API_PATH.format(options.model.value) +
            "?api-version=${options.apiVersion.value}"

    private fun RequestBuilder.auth() {
        val apiKey = OpenAICredentials.manager(ServiceProvider.Azure).credential
        tuner { it.setRequestProperty("api-key", apiKey) }
    }

    override fun chatCompletion(messages: List<ChatMessage>): ChatCompletion {
        val request = chatCompletionRequest(false) {
            this.messages = messages
        }
        return OpenAIHttp.post<ChatCompletion>(apiUrl, request) { auth() }
    }
}

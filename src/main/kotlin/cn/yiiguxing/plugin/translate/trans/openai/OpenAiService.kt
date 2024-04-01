package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import cn.yiiguxing.plugin.translate.trans.openai.chat.chatCompletionRequest
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder
import java.net.URLEncoder

const val DEFAULT_OPEN_AI_API_ENDPOINT = "https://api.openai.com"
const val OPEN_AI_API_PATH = "/v1/chat/completions"

private const val AZURE_OPEN_AI_API_PATH = "/openai/deployments/%s/chat/completions"

interface OpenAiService {

    @RequiresBackgroundThread
    fun chatCompletion(messages: List<ChatMessage>): ChatCompletion

    sealed interface Options<T> {
        val endpoint: String?
        val model: T
        val ttsModel: T
        val ttsVoice: OpenAiTtsVoice
        val ttsSpeed: Float
    }

    interface OpenAIOptions : Options<OpenAiModel>

    interface AzureOptions : Options<String?> {
        val apiVersion: AzureServiceVersion
    }

    companion object {
        fun get(options: Options<*>): OpenAiService {
            return when (options) {
                is OpenAIOptions -> OpenAI(options)
                is AzureOptions -> Azure(options)
            }
        }
    }
}


class OpenAI(private val options: OpenAiService.OpenAIOptions) : OpenAiService {
    private val apiUrl: String
        get() = (options.endpoint ?: DEFAULT_OPEN_AI_API_ENDPOINT).trimEnd('/') + OPEN_AI_API_PATH

    private fun RequestBuilder.auth() {
        val apiKey = OpenAiCredentials.manager(ServiceProvider.OpenAI).credential
        tuner { it.setRequestProperty("Authorization", "Bearer $apiKey") }
    }

    override fun chatCompletion(messages: List<ChatMessage>): ChatCompletion {
        val request = chatCompletionRequest {
            model = options.model.value
            this.messages = messages
        }
        return OpenAiHttp.post<ChatCompletion>(apiUrl, request) { auth() }
    }
}

class Azure(options: OpenAiService.AzureOptions) : OpenAiService {

    private val apiUrl: String = options.getApiUrl()

    private fun RequestBuilder.auth() {
        val apiKey = OpenAiCredentials.manager(ServiceProvider.Azure).credential
        tuner { it.setRequestProperty("api-key", apiKey) }
    }

    override fun chatCompletion(messages: List<ChatMessage>): ChatCompletion {
        val request = chatCompletionRequest(false) {
            this.messages = messages
        }
        return OpenAiHttp.post<ChatCompletion>(apiUrl, request) { auth() }
    }
}

private fun OpenAiService.AzureOptions.getApiUrl(): String {
    val baseUrl = requireNotNull(endpoint) { "Azure OpenAi API endpoint is required" }.trimEnd('/')
    val encodedDeploymentId = URLEncoder.encode(
        requireNotNull(model) { "Azure OpenAi model is required" },
        Charsets.UTF_8
    )
    val path = AZURE_OPEN_AI_API_PATH.format(encodedDeploymentId)
    return "$baseUrl$path?api-version=${apiVersion.value}"
}

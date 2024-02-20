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

interface OpenAIService {

    @RequiresBackgroundThread
    fun chatCompletion(messages: List<ChatMessage>): ChatCompletion

    sealed interface Options {
        val endpoint: String?
    }

    interface OpenAIOptions : Options {
        val model: OpenAIModel
    }

    interface AzureOptions : Options {
        val deploymentId: String?
        val apiVersion: AzureServiceVersion
    }

    companion object {
        fun get(options: Options): OpenAIService {
            return when (options) {
                is OpenAIOptions -> OpenAI(options)
                is AzureOptions -> Azure(options)
            }
        }
    }
}


class OpenAI(private val options: OpenAIService.OpenAIOptions) : OpenAIService {
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

    private val apiUrl: String = options.getApiUrl()

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

private fun OpenAIService.AzureOptions.getApiUrl(): String {
    val baseUrl = requireNotNull(endpoint) { "Azure OpenAI API endpoint is required" }.trimEnd('/')
    val encodedDeploymentId = URLEncoder.encode(
        requireNotNull(deploymentId) { "Azure OpenAI deployment id is required" },
        Charsets.UTF_8
    )
    val path = AZURE_OPEN_AI_API_PATH.format(encodedDeploymentId)
    return "$baseUrl$path?api-version=${apiVersion.value}"
}

package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.trans.openai.audio.SpeechRequest
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatCompletion
import cn.yiiguxing.plugin.translate.trans.openai.chat.chatCompletionRequest
import cn.yiiguxing.plugin.translate.trans.openai.prompt.Prompt
import cn.yiiguxing.plugin.translate.util.Http.sendJson
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.RequestBuilder
import java.net.URLEncoder

const val DEFAULT_OPEN_AI_API_ENDPOINT = "https://api.openai.com"
const val OPEN_AI_API_PATH = "/v1/chat/completions"
const val OPEN_AI_SPEECH_API_PATH = "/v1/audio/speech"

private const val AZURE_OPEN_AI_BASE_API_PATH = "openai/deployments"
private const val AZURE_OPEN_AI_CHAT_API_PATH = "chat/completions"
private const val AZURE_OPEN_AI_SPEECH_API_PATH = "audio/speech"

//TODO [Refactor] Separate translation service and TTS service
interface OpenAiService {

    /**
     * [Chat Completion](https://platform.openai.com/docs/api-reference/chat/create)
     */
    @RequiresBackgroundThread
    fun chatCompletion(prompt: Prompt): ChatCompletion

    /**
     * Generates audio from the input text.
     *
     * [Documentation](https://platform.openai.com/docs/api-reference/audio/createSpeech)
     */
    @RequiresBackgroundThread
    fun speech(text: String, indicator: ProgressIndicator? = null): ByteArray

    interface TTSBaseOptions {
        val ttsModel: OpenAiTTSModel
        val ttsVoice: OpenAiTtsVoice
        val ttsSpeed: Int
    }

    sealed interface Options {
        val endpoint: String?
    }

    interface OpenAIOptions : Options, TTSBaseOptions {
        val model: OpenAiGPTModel
        val customModel: String?
        val useCustomModel: Boolean
        val apiPath: String?
        val ttsEndpoint: String?
        val ttsApiPath: String?
        val useSeparateTtsApiSettings: Boolean
    }

    interface AzureOptions : Options, TTSBaseOptions {
        val deployment: String?
        val ttsDeployment: String?
        val apiVersion: AzureServiceVersion
        val ttsApiVersion: AzureServiceVersion
    }

    companion object {
        fun get(options: Options): OpenAiService {
            return when (options) {
                is OpenAIOptions -> OpenAI(options)
                is AzureOptions -> Azure(options)
            }
        }
    }
}


class OpenAI(private val options: OpenAiService.OpenAIOptions) : OpenAiService {
    private fun getApiUrl(endpoint: String?, path: String): String {
        return (endpoint ?: DEFAULT_OPEN_AI_API_ENDPOINT).trimEnd('/') + path
    }

    private fun RequestBuilder.auth(apiKey: String?) {
        tuner { it.setRequestProperty("Authorization", "Bearer ${apiKey ?: ""}") }
    }

    override fun chatCompletion(prompt: Prompt): ChatCompletion {
        val model = when {
            options.useCustomModel -> options.customModel
            else -> options.model.modelId
        }
        val request = chatCompletionRequest {
            this.model = model
            this.messages = prompt.messages
        }
        val path = options.apiPath?.trim()?.takeIf { it.isNotEmpty() } ?: OPEN_AI_API_PATH
        return OpenAiHttp.post(getApiUrl(options.endpoint, path), request) {
            auth(OpenAiCredentials.manager(ServiceProvider.OpenAI).credential)
        }
    }

    override fun speech(text: String, indicator: ProgressIndicator?): ByteArray {
        val supportedVoices = options.ttsModel.getSupportedVoices()
        val fixedVoice = options.ttsVoice.takeIf { it in supportedVoices } ?: supportedVoices.first()
        val request = SpeechRequest(
            model = options.ttsModel.modelId,
            input = text,
            voice = fixedVoice.value,
            speed = OpenAiTTSSpeed.get(options.ttsSpeed)
        )
        val endpoint = with(options) { if (useSeparateTtsApiSettings) ttsEndpoint else endpoint }
        val path = options.ttsApiPath?.trim()?.takeIf { it.isNotEmpty() } ?: OPEN_AI_SPEECH_API_PATH
        return OpenAiHttp.post(getApiUrl(endpoint, path)) {
            auth(OpenAiCredentials.manager(ServiceProvider.OpenAI, options.useSeparateTtsApiSettings).credential)
            sendJson(request) { it.readBytes(indicator) }
        }
    }
}

class Azure(private val options: OpenAiService.AzureOptions) : OpenAiService {

    private fun RequestBuilder.auth() {
        val apiKey = OpenAiCredentials.manager(ServiceProvider.Azure).credential
        tuner { it.setRequestProperty("api-key", apiKey) }
    }

    override fun chatCompletion(prompt: Prompt): ChatCompletion {
        val request = chatCompletionRequest(false) {
            this.messages = prompt.messages
        }
        return OpenAiHttp.post(options.getApiUrl(false), request) { auth() }
    }

    override fun speech(text: String, indicator: ProgressIndicator?): ByteArray {
        val request = SpeechRequest(
            model = options.ttsModel.modelId,
            input = text,
            voice = options.ttsVoice.value,
            speed = OpenAiTTSSpeed.get(options.ttsSpeed)
        )
        return OpenAiHttp.post(options.getApiUrl(true)) {
            auth()
            sendJson(request) { it.readBytes(indicator) }
        }
    }
}

private fun OpenAiService.AzureOptions.getApiUrl(tts: Boolean): String {
    val baseUrl = requireNotNull(endpoint) { "Azure OpenAi API endpoint is required" }.trimEnd('/')
    val encodedDeploymentId = URLEncoder.encode(
        requireNotNull(if (tts) ttsDeployment else deployment) { "Azure OpenAi deployment is required" },
        Charsets.UTF_8
    )
    val path = if (tts) AZURE_OPEN_AI_SPEECH_API_PATH else AZURE_OPEN_AI_CHAT_API_PATH
    return "$baseUrl/$AZURE_OPEN_AI_BASE_API_PATH/$encodedDeploymentId/$path?api-version=${apiVersion.value}"
}

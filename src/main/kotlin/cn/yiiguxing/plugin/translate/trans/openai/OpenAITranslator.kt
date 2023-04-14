package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatRole
import cn.yiiguxing.plugin.translate.trans.openai.chat.chatCompletionRequest
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.OPEN_AI
import cn.yiiguxing.plugin.translate.util.md5
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import org.jsoup.nodes.Document
import javax.swing.Icon

object OpenAITranslator : AbstractTranslator(), DocumentationTranslator {

    override val id: String = OPEN_AI.id
    override val name: String = OPEN_AI.translatorName
    override val icon: Icon = OPEN_AI.icon
    override val intervalLimit: Int = OPEN_AI.intervalLimit
    override val contentLengthLimit: Int = OPEN_AI.contentLengthLimit
    override val primaryLanguage: Lang get() = OPEN_AI.primaryLanguage
    override val supportedSourceLanguages: List<Lang> =
        OpenAILanguages.languages.toMutableList().apply { add(0, Lang.AUTO) }
    override val supportedTargetLanguages: List<Lang> = OpenAILanguages.languages

    private val openAIModel: OpenAIModel get() = service<OpenAISettings>().model


    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || !OpenAICredential.isApiKeySet) {
            return OPEN_AI.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        val model = openAIModel
        val cacheService = service<CacheService>()
        val cacheKey = getCacheKey(model, text, srcLang, targetLang)
        val cache = cacheService.getDiskCache(cacheKey)
        val translation = if (cache.isNullOrEmpty()) {
            val request = getChatCompletionRequest(model, text, srcLang, targetLang)
            val chatCompletion = OpenAI.chatCompletion(request)
            chatCompletion.choices.first().message!!.content.trim('"').also {
                cacheService.putDiskCache(cacheKey, it)
            }
        } else {
            cache
        }

        return Translation(text, translation, srcLang, targetLang)
    }

    override fun translateDocumentation(documentation: Document, srcLang: Lang, targetLang: Lang): Document {
        // TODO Call OpenAI API for translation.
        return documentation
    }

    private fun getChatCompletionRequest(
        openAIModel: OpenAIModel,
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isFofDocumentation: Boolean = false
    ) =
        chatCompletionRequest {
            model = openAIModel.value

            // TODO 配置文档翻译
            messages {
                message {
                    role = ChatRole.SYSTEM
                    content = "You are a translation engine that can only translate text and cannot interpret it."
                }
                message {
                    role = ChatRole.USER
                    content =
                        "Translate ${if (srcLang == Lang.AUTO) "" else "from ${srcLang.openAILanguage} "}to ${targetLang.openAILanguage}."
                }
                message {
                    role = ChatRole.USER
                    content = '"' + text + '"'
                }
            }
        }

    private fun getCacheKey(model: OpenAIModel, text: String, srcLang: Lang, targetLang: Lang): String {
        return "$id$model$text$srcLang$targetLang".md5()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is OpenAIStatusException) {
            if (throwable.statusCode == 401) {
                message("error.invalid.api.key")
            } else {
                throwable.error?.message
            }?.let {
                val action = ErrorInfo.continueAction(
                    message("action.check.configuration"),
                    icon = AllIcons.General.Settings
                ) {
                    OPEN_AI.showConfigurationDialog()
                }
                return ErrorInfo(it, action)
            }
        }

        return super.createErrorInfo(throwable)
    }
}
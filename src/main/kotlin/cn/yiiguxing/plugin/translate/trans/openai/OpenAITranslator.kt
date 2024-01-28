package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatRole
import cn.yiiguxing.plugin.translate.trans.openai.chat.chatMessages
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

    private val settings: OpenAISettings get() = service<OpenAISettings>()


    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || !OpenAICredentials.manager(settings.provider).isCredentialSet) {
            return OPEN_AI.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        val translation = translate(text, srcLang, targetLang, false)
        return Translation(text, translation, srcLang, targetLang)
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            checkContentLength(bodyHTML, contentLengthLimit)
            translate(bodyHTML, srcLang, targetLang, true)
        }
    }

    private fun translate(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isFofDocumentation: Boolean
    ): String {
        val cacheService = service<CacheService>()
        val cacheKey = getCacheKey(text, srcLang, targetLang)
        val cache = cacheService.getDiskCache(cacheKey)
        if (!cache.isNullOrEmpty()) {
            return cache
        }

        val request = getChatCompletionRequest(text, srcLang, targetLang, isFofDocumentation)
        val chatCompletion = OpenAIService.get(settings).chatCompletion(request)
        var result = chatCompletion.choices.first().message!!.content
        if (!isFofDocumentation && result.length > 1 && result.first() == '"' && result.last() == '"') {
            result = result.substring(1, result.lastIndex)
        }
        cacheService.putDiskCache(cacheKey, result)

        return result
    }

    private fun getChatCompletionRequest(
        text: String,
        srcLang: Lang,
        targetLang: Lang,
        isFofDocumentation: Boolean = false
    ) = chatMessages {
        message {
            role = ChatRole.SYSTEM
            content = "You are a translation engine that can " + if (isFofDocumentation) {
                "translate HTML document."
            } else {
                "only translate text and cannot interpret it."
            }
        }
        message {
            role = ChatRole.USER
            content =
                "Translate ${if (srcLang == Lang.AUTO) "" else "from ${srcLang.openAILanguage} "}to ${targetLang.openAILanguage}."
        }
        message {
            role = ChatRole.USER
            content = if (isFofDocumentation) text else """"$text""""
        }
    }

    private fun getCacheKey(text: String, srcLang: Lang, targetLang: Lang): String {
        val model = settings.model
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
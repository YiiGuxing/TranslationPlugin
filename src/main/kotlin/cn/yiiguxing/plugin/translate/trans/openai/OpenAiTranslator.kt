package cn.yiiguxing.plugin.translate.trans.openai

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.CacheService
import cn.yiiguxing.plugin.translate.trans.*
import cn.yiiguxing.plugin.translate.trans.documentation.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.documentation.translateBody
import cn.yiiguxing.plugin.translate.trans.openai.config.OpenAiRequestConfigService
import cn.yiiguxing.plugin.translate.trans.openai.exception.OpenAIStatusException
import cn.yiiguxing.plugin.translate.trans.openai.prompt.EmptyPromptException
import cn.yiiguxing.plugin.translate.trans.openai.prompt.Prompt
import cn.yiiguxing.plugin.translate.trans.openai.prompt.PromptService
import cn.yiiguxing.plugin.translate.trans.openai.prompt.checkNotEmpty
import cn.yiiguxing.plugin.translate.trans.openai.prompt.template.TemplateRenderException
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine.OPEN_AI
import cn.yiiguxing.plugin.translate.util.md5
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import org.jsoup.nodes.Document
import javax.swing.Icon

object OpenAiTranslator : AbstractTranslator(), DocumentationTranslator {

    override val id: String = OPEN_AI.id
    override val name: String = OPEN_AI.translatorName
    override val icon: Icon = OPEN_AI.icon

    @Deprecated("""Use "RateLimiter" in the "translate" implementation.""")
    override val intervalLimit: Int = OPEN_AI.intervalLimit
    override val primaryLanguage: Lang get() = OPEN_AI.primaryLanguage
    override val supportedSourceLanguages: List<Lang> get() = OpenAiSupportedLanguages.sourceLanguages
    override val supportedTargetLanguages: List<Lang> get() = OpenAiSupportedLanguages.targetLanguages

    private val settings: OpenAiSettings get() = service<OpenAiSettings>()
    private val promptService: PromptService get() = service<PromptService>()
    private val requestConfigService: OpenAiRequestConfigService get() = service<OpenAiRequestConfigService>()

    private val currentModelId: String?
        get() = when (val options = settings.getOptions()) {
            is OpenAiService.OpenAIOptions -> options.modelId
            is OpenAiService.AzureOptions -> null
        }


    override fun translationCacheToken(cacheType: TranslationCacheType): String? =
        when (val options = settings.getOptions(settings.provider)) {
            is OpenAiService.OpenAIOptions -> {
                "model=${options.modelId};config=${requestConfigService.configHash() ?: ""}"
            }

            is OpenAiService.AzureOptions -> {
                "deployment=${options.deployment ?: ""};apiVersion=${options.apiVersion.value}"
            }
        }

    override fun checkConfiguration(force: Boolean): Boolean {
        if (force || !OPEN_AI.isConfigured()) {
            return OPEN_AI.showConfigurationDialog()
        }

        return true
    }

    override fun doTranslate(text: String, srcLang: Lang, targetLang: Lang): Translation {
        val prompt = promptService.let { service ->
            service.getTranslatorPromptTemplate(text, srcLang, targetLang, currentModelId)
                .let { service.render(it, currentModelId) }
        }
        val translation = translate(prompt, text, srcLang, targetLang)
        return Translation(text, translation, srcLang, targetLang)
    }

    override fun translateDocumentation(
        documentation: Document,
        srcLang: Lang,
        targetLang: Lang
    ): Document = checkError {
        documentation.translateBody { bodyHTML ->
            val prompt = promptService.let { service ->
                service.getDocumentPromptTemplate(bodyHTML, srcLang, targetLang, currentModelId)
                    .let { service.render(it, currentModelId) }
            }
            translate(prompt, bodyHTML, srcLang, targetLang).trim().let { translated ->
                if (translated.startsWith("```html\n") && translated.endsWith("\n```")) {
                    translated.substring(8, translated.length - 4)
                } else {
                    translated
                }
            }
        }
    }

    private fun translate(prompt: Prompt, text: String, srcLang: Lang, targetLang: Lang): String {
        prompt.checkNotEmpty()

        val cacheService = service<CacheService>()
        val cacheKey = getDiskCacheKey(prompt, srcLang, targetLang)
        val cache = cacheService.getDiskCache(cacheKey)
        if (!cache.isNullOrEmpty()) {
            return cache
        }

        val chatCompletion = OpenAiService.get(settings.getOptions())
            .chatCompletion(prompt, ChatRequestContext(text, srcLang, targetLang))
        val result = chatCompletion.choices?.first()?.message?.content ?: return ""
        cacheService.putDiskCache(cacheKey, result)

        return result
    }

    private fun getDiskCacheKey(prompt: Prompt, srcLang: Lang, targetLang: Lang): String {
        val text = prompt.messages.joinToString(";") { "${it.role}:${it.content}" }
        return "$id;${translationCacheToken(TranslationCacheType.TEXT)};$text;$srcLang;$targetLang".md5()
    }

    override fun createErrorInfo(throwable: Throwable): ErrorInfo? {
        if (throwable is TemplateRenderException) {
            return ErrorInfo(throwable.message)
        }
        if (throwable is EmptyPromptException) {
            return ErrorInfo("Empty prompt")
        }

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
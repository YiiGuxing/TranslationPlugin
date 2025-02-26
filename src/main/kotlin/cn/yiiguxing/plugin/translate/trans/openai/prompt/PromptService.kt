package cn.yiiguxing.plugin.translate.trans.openai.prompt

import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.openai.prompt.template.TemplateRenderException
import cn.yiiguxing.plugin.translate.trans.openai.prompt.template.TemplateVariable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.context.Context
import org.apache.velocity.exception.VelocityException
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private const val DEFAULT_TRANSLATOR_PROMPT_TEMPLATE = """
[SYSTEM]
You are a translator.
The user will provide you with text in triple quotes.
Translate the text#if(${'$'}LANGUAGE.isExplicit(${'$'}SOURCE_LANGUAGE)) from ${'$'}{SOURCE_LANGUAGE.languageName} to#else into#end ${'$'}{TARGET_LANGUAGE.languageName}.
Do not return the translated text in triple quotes.
[USER]
""${'"'}
${'$'}TEXT
""${'"'}
"""

private const val DEFAULT_DOCUMENT_PROMPT_TEMPLATE = """
[SYSTEM]
You are an html document translator.
The user will provide you with an html document.
Translate the html document#if(${'$'}LANGUAGE.isExplicit(${'$'}SOURCE_LANGUAGE)) from ${'$'}{SOURCE_LANGUAGE.languageName} to#else into#end ${'$'}{TARGET_LANGUAGE.languageName}.
Do not translate the content inside "pre" and "code" tags.
[USER]
${'$'}TEXT
"""

private const val PROMPTS_DIRECTORY = "openai"
private const val TRANSLATOR_PROMPT = "translator.prompt"
private const val DOCUMENT_PROMPT = "document.prompt"

private val LOG = logger<PromptService>()

/**
 * Prompt Service.
 */
@Service
class PromptService {

    private val templateEngine: VelocityEngine by lazy {
        VelocityEngine().apply {
            setProperty(Velocity.RUNTIME_REFERENCES_STRICT, true)
            setProperty(Velocity.RUNTIME_REFERENCES_STRICT_ESCAPE, true)
            setProperty(Velocity.ENCODING_DEFAULT, Charsets.UTF_8.name())
            init()
        }
    }
    private val commonContext: Context by lazy {
        val contextData = TemplateVariable.commonVariables().mapKeys { it.key.name }
        VelocityContext(Collections.unmodifiableMap(contextData))
    }


    /**
     * Get a prompt template for the translator.
     *
     * [Apache Velocity](https://velocity.apache.org/engine/devel/user-guide.html) template language is used.
     */
    @RequiresBackgroundThread
    fun getTranslatorPromptTemplate(sourceText: String, sourceLang: Lang, targetLang: Lang): PromptTemplate {
        val template = getTemplate(TRANSLATOR_PROMPT) ?: DEFAULT_TRANSLATOR_PROMPT_TEMPLATE
        return PromptTemplate(template, sourceText, sourceLang, targetLang)
    }

    /**
     * Get a prompt template for the document translator.
     *
     * [Apache Velocity](https://velocity.apache.org/engine/devel/user-guide.html) template language is used.
     */
    @RequiresBackgroundThread
    fun getDocumentPromptTemplate(doc: String, sourceLang: Lang, targetLang: Lang): PromptTemplate {
        val template = getTemplate(DOCUMENT_PROMPT) ?: DEFAULT_DOCUMENT_PROMPT_TEMPLATE
        return PromptTemplate(template, doc, sourceLang, targetLang)
    }

    @RequiresBackgroundThread
    private fun getTemplate(name: String): String? {
        val ptFile = TranslationStorages.DATA_DIRECTORY.resolve(Paths.get(PROMPTS_DIRECTORY, name))
        return try {
            if (Files.exists(ptFile) && Files.isRegularFile(ptFile)) {
                Files.readString(ptFile)
            } else {
                LOG.debug("Prompt template file not found: $name")
                null
            }
        } catch (e: Exception) {
            LOG.warn("Failed to read prompt template file: $name", e)
            null
        }
    }

    /**
     * Render the prompt template.
     */
    fun render(template: PromptTemplate): Prompt {
        LOG.debug("Rendering prompt template: $template")

        val context = VelocityContext(commonContext).apply {
            put(TemplateVariable.TEXT.name, template.sourceText)
            put(TemplateVariable.SOURCE_LANGUAGE.name, template.sourceLanguage)
            put(TemplateVariable.TARGET_LANGUAGE.name, template.targetLanguage)
        }

        val promptWriter = StringWriter()
        try {
            templateEngine.evaluate(context, promptWriter, PromptService::class.java.name, template.template)
        } catch (e: VelocityException) {
            val message = "Failed to render prompt template: ${e.message}"
            LOG.warn(message, e)

            throw TemplateRenderException(message, e)
        }

        val prompt = promptWriter.toString()
        LOG.debug("Rendered prompt:\n$prompt")

        return PromptParser.parse(prompt).also {
            LOG.debug("Parsed prompt: $it")
        }
    }
}


package cn.yiiguxing.plugin.translate.trans.openai.config

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The structure of the request configuration file for customizing
 * chat completion requests of OpenAI-compatible APIs.
 *
 * @see OpenAiRequestConfigService
 */
internal data class OpenAiRequestConfig(
    val version: Int? = null,
    val default: ModelConfig? = null,
    val models: Map<String, ModelConfig>? = null,
) {

    internal data class PromptConfig(
        val translator: String? = null,
        val document: String? = null,
    )

    internal data class ModelConfig(
        val languageMapping: Map<String, String>? = null,
        val prompt: PromptConfig? = null,
        val headers: Map<String, String>? = null,
        val body: Map<String, Any?>? = null,
    )
}

private fun OpenAiRequestConfig.ModelConfig?.orEmpty(): OpenAiRequestConfig.ModelConfig =
    this ?: OpenAiRequestConfig.ModelConfig()

/**
 * Builds the final chat completion request body.
 *
 * The [model] and [messages] are always forced and override
 * any same-named fields in the [configuredBody].
 */
internal fun buildChatRequestBody(
    configuredBody: Map<String, Any?>,
    model: String,
    messages: List<ChatMessage>,
): Map<String, Any?> {
    val body = LinkedHashMap<String, Any?>(configuredBody)
    body["model"] = model
    body["messages"] = messages
    return body
}

/**
 * Resolves the [pathString] to a file path.
 * Relative paths are resolved against the [configDirectory].
 */
internal fun resolvePromptTemplatePath(configDirectory: Path, pathString: String): Path {
    val path = Paths.get(pathString)
    return if (path.isAbsolute) path else configDirectory.resolve(path)
}

/**
 * Resolves request configurations, without I/O.
 */
internal object OpenAiRequestConfigResolver {

    const val TEXT_PLACEHOLDER = "TEXT"
    const val SOURCE_LANGUAGE_PLACEHOLDER = "SOURCE_LANGUAGE"
    const val TARGET_LANGUAGE_PLACEHOLDER = "TARGET_LANGUAGE"

    /**
     * Escapes the placeholder prefix so that a literal `${` can be expressed as `$${`.
     */
    const val ESCAPED_PLACEHOLDER_PREFIX = "$\${"

    private val PLACEHOLDER_REGEX = Regex("\\$\\{(TEXT|SOURCE_LANGUAGE|TARGET_LANGUAGE)}")
    private const val PLACEHOLDER_MARKER = "\u0000"

    /**
     * Merges the [model] configuration into the [default] configuration, deeply.
     * Entries of the same key in the [model] configuration override the [default] configuration.
     */
    fun merge(
        default: OpenAiRequestConfig.ModelConfig?,
        model: OpenAiRequestConfig.ModelConfig?
    ): OpenAiRequestConfig.ModelConfig {
        val defaultConfig = default.orEmpty()
        val modelConfig = model.orEmpty()
        val defaultPrompt = defaultConfig.prompt
        val modelPrompt = modelConfig.prompt
        return OpenAiRequestConfig.ModelConfig(
            languageMapping = defaultConfig.languageMapping.orEmpty() + modelConfig.languageMapping.orEmpty(),
            prompt = OpenAiRequestConfig.PromptConfig(
                translator = modelPrompt?.translator ?: defaultPrompt?.translator,
                document = modelPrompt?.document ?: defaultPrompt?.document,
            ).takeIf { it.translator != null || it.document != null },
            headers = defaultConfig.headers.orEmpty() + modelConfig.headers.orEmpty(),
            body = deepMerge(defaultConfig.body.orEmpty(), modelConfig.body.orEmpty()),
        )
    }

    /**
     * Maps the [lang] to a provider-specific language definition.
     *
     * The [languageMapping] keys are the [Lang] enum names (e.g. `CHINESE_SIMPLIFIED`)
     * or the [language codes][Lang.code] (e.g. `zh-CN`). Unmapped languages fall back
     * to the [language's English name][Lang.languageName].
     */
    fun mapLanguage(languageMapping: Map<String, String>?, lang: Lang): String {
        val mapping = languageMapping.orEmpty()
        return mapping[lang.name] ?: mapping[lang.code] ?: lang.languageName
    }

    /**
     * Resolves the placeholders in the [value], recursively.
     *
     * Only `${TEXT}`, `${SOURCE_LANGUAGE}` and `${TARGET_LANGUAGE}` are resolved;
     * other `${...}` sequences are kept as is. Use `$${...}` to escape a literal `${...}`.
     */
    fun resolvePlaceholders(
        value: Any?,
        text: String,
        sourceLanguage: Lang,
        targetLanguage: Lang,
        languageMapping: Map<String, String>?
    ): Any? = when (value) {
        is String -> resolveStringPlaceholders(value) { placeholder ->
            when (placeholder) {
                TEXT_PLACEHOLDER -> text
                SOURCE_LANGUAGE_PLACEHOLDER -> mapLanguage(languageMapping, sourceLanguage)
                TARGET_LANGUAGE_PLACEHOLDER -> mapLanguage(languageMapping, targetLanguage)
                else -> null
            }
        }

        is Map<*, *> -> value.mapValues { (_, v) ->
            resolvePlaceholders(v, text, sourceLanguage, targetLanguage, languageMapping)
        }

        is List<*> -> value.map {
            resolvePlaceholders(it, text, sourceLanguage, targetLanguage, languageMapping)
        }

        else -> value
    }

    private fun resolveStringPlaceholders(value: String, resolver: (String) -> String?): String {
        val escaped = value.replace(ESCAPED_PLACEHOLDER_PREFIX, PLACEHOLDER_MARKER)
        val resolved = escaped.replace(PLACEHOLDER_REGEX) { matchResult ->
            resolver(matchResult.groupValues[1]) ?: matchResult.value
        }
        return resolved.replace(PLACEHOLDER_MARKER, "\${")
    }

    private fun deepMerge(base: Map<String, Any?>, override: Map<String, Any?>): Map<String, Any?> {
        val result = LinkedHashMap(base)
        for ((key, value) in override) {
            val baseValue = result[key]
            if (baseValue is Map<*, *> && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                result[key] = deepMerge(baseValue as Map<String, Any?>, value as Map<String, Any?>)
            } else {
                result[key] = value
            }
        }
        return result
    }
}

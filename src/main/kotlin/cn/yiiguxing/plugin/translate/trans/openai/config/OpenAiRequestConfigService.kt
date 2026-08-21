package cn.yiiguxing.plugin.translate.trans.openai.config

import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.openai.ChatRequestContext
import cn.yiiguxing.plugin.translate.util.Http
import cn.yiiguxing.plugin.translate.util.md5
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

/**
 * Loads and resolves the request configuration file for customizing
 * chat completion requests of OpenAI-compatible APIs.
 *
 * The configuration file is a JSON file located at [CONFIG_FILE].
 */
@Service
class OpenAiRequestConfigService {

    companion object {
        private const val CONFIG_DIRECTORY_NAME = "openai"
        const val CONFIG_FILE_NAME = "config.json"
        const val SCHEMA_FILE_NAME = "openai-config.schema.json"
        private const val SCHEMA_RESOURCE_PATH = "/schemas/openai-config.schema.json"
        private const val SCHEMA_FIELD = "\$schema"
        private const val DEFAULT_SCHEMA_REFERENCE = "./openai-config.schema.json"

        @JvmField
        val CONFIG_DIRECTORY: Path = TranslationStorages.DATA_DIRECTORY.resolve(CONFIG_DIRECTORY_NAME)

        @JvmField
        val CONFIG_FILE: Path = CONFIG_DIRECTORY.resolve(CONFIG_FILE_NAME)

        @JvmField
        val SCHEMA_FILE: Path = CONFIG_DIRECTORY.resolve(SCHEMA_FILE_NAME)

        private const val DEFAULT_CONFIG_CONTENT = "{\n" +
                "  \"${'$'}schema\": \"$DEFAULT_SCHEMA_REFERENCE\",\n" +
                "  \"version\": 1,\n" +
                "  \"default\": {},\n" +
                "  \"models\": {}\n" +
                "}"

        private val LOG: Logger = logger<OpenAiRequestConfigService>()

        private val PRETTY_GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        /**
         * Prepares the config files for editing:
         * - copies the bundled JSON schema to [SCHEMA_FILE], overwriting it;
         * - creates [CONFIG_FILE] with the default content if it is missing;
         * - adds the `$schema` field to [CONFIG_FILE] if it is missing.
         *
         * @return `true` if succeeded, `false` otherwise.
         */
        fun prepareConfigFilesForEditing(): Boolean {
            return try {
                installSchemaFile()
                ensureConfigFile()
                true
            } catch (e: Exception) {
                LOG.warn("Failed to prepare the OpenAI request config files for editing.", e)
                false
            }
        }

        private fun installSchemaFile() {
            val schemaContent = OpenAiRequestConfigService::class.java
                .getResourceAsStream(SCHEMA_RESOURCE_PATH)
                ?.use { input -> input.readBytes().toString(Charsets.UTF_8) }
                ?: throw IOException("The JSON schema resource is missing: $SCHEMA_RESOURCE_PATH")

            Files.createDirectories(CONFIG_DIRECTORY)
            Files.write(SCHEMA_FILE, schemaContent.toByteArray(Charsets.UTF_8))
        }

        private fun ensureConfigFile() {
            Files.createDirectories(CONFIG_DIRECTORY)
            if (!Files.exists(CONFIG_FILE)) {
                Files.write(CONFIG_FILE, DEFAULT_CONFIG_CONTENT.toByteArray(Charsets.UTF_8))
            } else {
                ensureSchemaField()
            }
        }

        private fun ensureSchemaField() {
            val content = Files.readString(CONFIG_FILE)
            val json = try {
                Http.defaultGson.fromJson(content, JsonObject::class.java)
            } catch (e: JsonParseException) {
                LOG.warn("Failed to parse the OpenAI request config file: $CONFIG_FILE", e)
                return
            }
            if (json.has(SCHEMA_FIELD)) {
                return
            }

            json.add(SCHEMA_FIELD, JsonPrimitive(DEFAULT_SCHEMA_REFERENCE))
            Files.write(CONFIG_FILE, PRETTY_GSON.toJson(json).toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * The prompt template kinds.
     */
    enum class PromptKind(val fileName: String) {
        TRANSLATOR("translator.prompt"),
        DOCUMENT("document.prompt");
    }

    private data class CachedConfig(
        val config: OpenAiRequestConfig?,
        val lastModified: FileTime?,
        val size: Long,
    )

    @Volatile
    private var cached: CachedConfig? = null

    /**
     * Resolves the request body for the [modelId], with placeholders resolved.
     *
     * The resolved body may contain the `model` field from the configuration,
     * which overrides the model id selected in the UI. The final request body
     * is built by [buildChatRequestBody], which forces the `messages` field
     * and the `stream` field.
     */
    @RequiresBackgroundThread
    fun resolveRequestBody(modelId: String, context: ChatRequestContext): Map<String, Any?> {
        val config = loadConfig().config ?: return emptyMap()
        val mergedConfig = OpenAiRequestConfigResolver.merge(config.default, config.models?.get(modelId))
        val languageMapping = mergedConfig.languageMapping
        val body = LinkedHashMap<String, Any?>()
        mergedConfig.body
            .orEmpty()
            .forEach { (key, value) ->
                body[key] = OpenAiRequestConfigResolver.resolvePlaceholders(
                    value,
                    context.text,
                    context.sourceLanguage,
                    context.targetLanguage,
                    languageMapping
                )
            }
        return body
    }

    /**
     * Resolves the request headers for the [modelId], with placeholders resolved.
     */
    @RequiresBackgroundThread
    fun resolveRequestHeaders(modelId: String, context: ChatRequestContext): Map<String, String> {
        val config = loadConfig().config ?: return emptyMap()
        val mergedConfig = OpenAiRequestConfigResolver.merge(config.default, config.models?.get(modelId))
        val languageMapping = mergedConfig.languageMapping
        return mergedConfig.headers
            .orEmpty()
            .mapValues { (_, value) ->
                OpenAiRequestConfigResolver.resolvePlaceholders(
                    value,
                    context.text,
                    context.sourceLanguage,
                    context.targetLanguage,
                    languageMapping
                ).toString()
            }
    }

    /**
     * Maps the [lang] to a provider-specific language definition
     * by the language mapping table of the merged configuration for the [modelId].
     */
    @RequiresBackgroundThread
    fun mapLanguage(modelId: String?, lang: Lang): String {
        val config = loadConfig().config ?: return lang.languageName
        val mergedConfig = OpenAiRequestConfigResolver.merge(config.default, modelId?.let { config.models?.get(it) })
        return OpenAiRequestConfigResolver.mapLanguage(mergedConfig.languageMapping, lang)
    }

    /**
     * Resolves the configured prompt template file path for the [kind]
     * in the merged configuration for the [modelId],
     * or `null` if it is not configured. Relative paths are resolved
     * against the config file directory.
     */
    @RequiresBackgroundThread
    fun getPromptTemplatePath(kind: PromptKind, modelId: String?): Path? {
        val config = loadConfig().config ?: return null
        val mergedConfig = OpenAiRequestConfigResolver.merge(config.default, modelId?.let { config.models?.get(it) })
        val promptConfig = mergedConfig.prompt ?: return null
        val pathString = when (kind) {
            PromptKind.TRANSLATOR -> promptConfig.translator
            PromptKind.DOCUMENT -> promptConfig.document
        }?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        return resolvePromptTemplatePath(CONFIG_DIRECTORY, pathString)
    }

    /**
     * Returns the hash of the config file content,
     * or `null` if the config file doesn't exist.
     */
    fun configHash(): String? {
        val file = CONFIG_FILE
        if (!Files.isRegularFile(file)) {
            return null
        }
        return try {
            Files.readString(file).md5()
        } catch (_: Exception) {
            LOG.warn("Failed to read the OpenAI request config file: $file")
            null
        }
    }

    @RequiresBackgroundThread
    private fun loadConfig(): CachedConfig {
        val file = CONFIG_FILE
        val fileAttrs = try {
            Files.readAttributes(file, BasicFileAttributes::class.java)
        } catch (_: Exception) {
            null
        }

        val cachedConfig = cached
        if (fileAttrs != null && cachedConfig != null &&
            cachedConfig.lastModified == fileAttrs.lastModifiedTime() &&
            cachedConfig.size == fileAttrs.size()
        ) {
            return cachedConfig
        }

        val config = if (fileAttrs == null) {
            LOG.debug("OpenAI request config file not found: $file")
            null
        } else {
            try {
                val content = Files.readString(file)
                Http.defaultGson.fromJson(content, OpenAiRequestConfig::class.java)
            } catch (e: Exception) {
                LOG.warn("Failed to parse the OpenAI request config file: $file", e)
                null
            }
        }

        return CachedConfig(config, fileAttrs?.lastModifiedTime(), fileAttrs?.size() ?: -1)
            .also { cached = it }
    }
}

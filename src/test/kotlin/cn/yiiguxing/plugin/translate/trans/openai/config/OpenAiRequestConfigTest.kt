package cn.yiiguxing.plugin.translate.trans.openai.config

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatRole
import cn.yiiguxing.plugin.translate.util.Http
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.file.Paths

/**
 * OpenAiRequestConfigTest
 */
class OpenAiRequestConfigTest {

    @Test
    fun testConfigParsing() {
        val json = """
            {
              "version": 1,
              "default": {
                "languageMapping": {
                  "zh-CN": "Simplified Chinese",
                  "CHINESE_TRADITIONAL": "Traditional Chinese"
                },
                "prompt": {
                  "translator": "prompts/translator.prompt",
                  "document": "prompts/document.prompt"
                },
                "headers": { "X-Api-Version": "2024-01-01" },
                "body": { "temperature": 0.3 }
              },
              "models": {
                "gpt-5.4-mini": {
                  "languageMapping": {
                    "zh-CN": "Chinese"
                  },
                  "prompt": {
                    "translator": "prompts/mini-translator.prompt"
                  },
                  "body": { "temperature": 0.7 }
                }
              }
            }
        """.trimIndent()

        val config = Http.defaultGson.fromJson(json, OpenAiRequestConfig::class.java)
        assertNotNull(config)
        assertEquals(1, config.version)
        assertEquals("Simplified Chinese", config.default?.languageMapping?.get("zh-CN"))
        assertEquals("Traditional Chinese", config.default?.languageMapping?.get("CHINESE_TRADITIONAL"))
        assertEquals("prompts/translator.prompt", config.default?.prompt?.translator)
        assertEquals("prompts/document.prompt", config.default?.prompt?.document)
        assertEquals("2024-01-01", config.default?.headers?.get("X-Api-Version"))
        assertEquals(0.3, config.default?.body?.get("temperature"))
        assertEquals("Chinese", config.models?.get("gpt-5.4-mini")?.languageMapping?.get("zh-CN"))
        assertEquals("prompts/mini-translator.prompt", config.models?.get("gpt-5.4-mini")?.prompt?.translator)
        assertEquals(0.7, config.models?.get("gpt-5.4-mini")?.body?.get("temperature"))
    }

    @Test
    fun testMergeConfigs() {
        val default = OpenAiRequestConfig.ModelConfig(
            languageMapping = mapOf("zh-CN" to "Simplified Chinese", "en" to "English"),
            prompt = OpenAiRequestConfig.PromptConfig(
                translator = "prompts/translator.prompt",
                document = "prompts/document.prompt"
            ),
            headers = mapOf("X-A" to "1", "X-B" to "2"),
            body = mapOf(
                "temperature" to 0.3,
                "nested" to mapOf("a" to 1, "b" to 2),
                "list" to listOf(1, 2)
            )
        )
        val model = OpenAiRequestConfig.ModelConfig(
            languageMapping = mapOf("zh-CN" to "Chinese", "ja" to "Japanese"),
            prompt = OpenAiRequestConfig.PromptConfig(
                translator = "prompts/mini-translator.prompt"
            ),
            headers = mapOf("X-B" to "20", "X-C" to "3"),
            body = mapOf(
                "temperature" to 0.7,
                "nested" to mapOf("b" to 20, "c" to 3)
            )
        )

        val merged = OpenAiRequestConfigResolver.merge(default, model)
        assertEquals(
            mapOf("zh-CN" to "Chinese", "en" to "English", "ja" to "Japanese"),
            merged.languageMapping
        )
        // The model's translator overrides the default, the document falls back to the default.
        assertEquals("prompts/mini-translator.prompt", merged.prompt?.translator)
        assertEquals("prompts/document.prompt", merged.prompt?.document)
        assertEquals(mapOf("X-A" to "1", "X-B" to "20", "X-C" to "3"), merged.headers)

        val mergedBody = merged.body
        assertNotNull(mergedBody)
        assertEquals(0.7, mergedBody?.get("temperature"))
        assertEquals(listOf(1, 2), mergedBody?.get("list"))

        @Suppress("UNCHECKED_CAST")
        val nested = mergedBody?.get("nested") as Map<String, Any?>
        assertEquals(1, nested["a"])
        assertEquals(20, nested["b"])
        assertEquals(3, nested["c"])
    }

    @Test
    fun testResolvePlaceholders() {
        val value = mapOf(
            "text" to "Hello \${TEXT}",
            "source" to "\${SOURCE_LANGUAGE}",
            "target" to "\${TARGET_LANGUAGE}",
            "unknown" to "\${UNKNOWN}",
            "nested" to mapOf("nested-text" to "\${TEXT}"),
            "list" to listOf("\${TARGET_LANGUAGE}", 123, true)
        )
        val languageMapping = mapOf("zh-CN" to "中文", "en" to "英语")

        val resolved = OpenAiRequestConfigResolver.resolvePlaceholders(
            value,
            "hello",
            Lang.CHINESE_SIMPLIFIED,
            Lang.ENGLISH,
            languageMapping
        )
        @Suppress("UNCHECKED_CAST")
        val resolvedMap = resolved as Map<String, Any?>
        assertEquals("Hello hello", resolvedMap["text"])
        assertEquals("中文", resolvedMap["source"])
        assertEquals("英语", resolvedMap["target"])
        assertEquals("\${UNKNOWN}", resolvedMap["unknown"])

        @Suppress("UNCHECKED_CAST")
        val nested = resolvedMap["nested"] as Map<String, Any?>
        assertEquals("hello", nested["nested-text"])

        @Suppress("UNCHECKED_CAST")
        val list = resolvedMap["list"] as List<Any?>
        assertEquals("英语", list[0])
        assertEquals(123, list[1])
        assertEquals(true, list[2])
    }

    @Test
    fun testEscapedPlaceholder() {
        val value = "\$\${TEXT} and \${TEXT}"

        val resolved = OpenAiRequestConfigResolver.resolvePlaceholders(
            value,
            "hello",
            Lang.AUTO,
            Lang.ENGLISH,
            null
        )
        assertEquals("\${TEXT} and hello", resolved)
    }

    @Test
    fun testMapLanguage() {
        val mapping = mapOf(
            "CHINESE_SIMPLIFIED" to "中文(简体)",
            "zh-TW" to "中文(繁體)",
            "en" to "英语"
        )

        // The Lang name takes precedence.
        assertEquals("中文(简体)", OpenAiRequestConfigResolver.mapLanguage(mapping, Lang.CHINESE_SIMPLIFIED))
        // Falls back to the Lang code.
        assertEquals("中文(繁體)", OpenAiRequestConfigResolver.mapLanguage(mapping, Lang.CHINESE_TRADITIONAL))
        assertEquals("英语", OpenAiRequestConfigResolver.mapLanguage(mapping, Lang.ENGLISH))
        // Falls back to the language's English name.
        assertEquals(Lang.JAPANESE.languageName, OpenAiRequestConfigResolver.mapLanguage(mapping, Lang.JAPANESE))
        // Empty or null mapping falls back to the language's English name.
        assertEquals(Lang.CHINESE_SIMPLIFIED.languageName, OpenAiRequestConfigResolver.mapLanguage(null, Lang.CHINESE_SIMPLIFIED))
        assertEquals(Lang.CHINESE_SIMPLIFIED.languageName, OpenAiRequestConfigResolver.mapLanguage(emptyMap(), Lang.CHINESE_SIMPLIFIED))
    }

    @Test
    fun testBuildChatRequestBodyForcesModelAndMessages() {
        val configuredBody = mapOf(
            "model" to "configured-model",
            "messages" to listOf("configured"),
            "temperature" to 0.5
        )
        val messages = listOf(ChatMessage(ChatRole.USER, "hello"))

        val body = buildChatRequestBody(configuredBody, "gpt-5.4-mini", messages)
        assertEquals("gpt-5.4-mini", body["model"])
        assertEquals(messages, body["messages"])
        assertEquals(0.5, body["temperature"])
    }

    @Test
    fun testResolvePromptTemplatePath() {
        val configDirectory = Paths.get("/config/dir")

        assertEquals(
            Paths.get("/config/dir", "prompts", "translator.prompt"),
            resolvePromptTemplatePath(configDirectory, "prompts/translator.prompt")
        )
        assertEquals(
            Paths.get("/absolute", "path", "translator.prompt"),
            resolvePromptTemplatePath(configDirectory, "/absolute/path/translator.prompt")
        )
    }
}

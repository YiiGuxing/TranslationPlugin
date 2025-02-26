package cn.yiiguxing.plugin.translate.trans.openai.prompt

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatRole

/**
 * Prompt parser.
 */
object PromptParser {

    private const val ROLE_START = '['
    private const val ROLE_END = ']'
    private val MESSAGE_HEAD_TRIMMER_REGEX = Regex("^\\s*\n")

    /**
     * Parses the [input] string to a [Prompt].
     *
     * The input string should be formatted as follows:
     * ```
     * [ROLE1]
     * Message 1
     * [ROLE2]
     * Message 2
     * ...
     * ```
     * The allowed roles see [ChatRole].
     */
    fun parse(input: String): Prompt {
        val lines = input.lines()
        var chatRole = ChatRole.USER
        val contentBuilder = StringBuilder()
        val messages = mutableListOf<ChatMessage>()
        for (line in lines) {
            if (line.startsWith(ROLE_START) && line.endsWith(ROLE_END)) {
                val role = ChatRole.from(line.substring(1, line.length - 1))
                if (role == null) {
                    contentBuilder.appendLine(line)
                    continue
                }

                val content = contentBuilder.flush()
                if (content.isNotEmpty()) {
                    messages.add(ChatMessage(chatRole, content))
                }

                chatRole = role
            } else {
                contentBuilder.appendLine(line)
            }
        }

        val content = contentBuilder.flush()
        if (content.isNotEmpty()) {
            messages.add(ChatMessage(chatRole, content))
        }

        return Prompt(messages)
    }

    private fun StringBuilder.flush(): String {
        val content = toString()
            .replace(MESSAGE_HEAD_TRIMMER_REGEX, "")
            .trimEnd()
            .trimIndent()
        setLength(0)
        return content
    }
}
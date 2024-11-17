package cn.yiiguxing.plugin.translate.trans.openai.prompt

import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatMessage
import cn.yiiguxing.plugin.translate.trans.openai.chat.ChatRole

data class Prompt(val messages: List<ChatMessage>) {
    constructor(vararg messages: ChatMessage) : this(messages.toList())

    @Suppress("unused")
    constructor(contents: String) : this(ChatMessage(ChatRole.USER, contents))
}

/**
 * Checks if the prompt is not empty.
 *
 * @throws EmptyPromptException if the prompt is empty.
 */
fun Prompt.checkNotEmpty() {
    if (messages.isEmpty()) {
        throw EmptyPromptException()
    }
}

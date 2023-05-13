@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai.chat

import com.google.gson.annotations.SerializedName

/**
 * The messages to generate chat completions for.
 *
 * [documentation](https://platform.openai.com/docs/api-reference/chat/create)
 */
data class ChatMessage(
    /**
     * The role of the message author.
     */
    @SerializedName("role") val role: ChatRole,

    /**
     * The contents of the message.
     */
    @SerializedName("content") val content: String,
)

/**
 * The messages to generate chat completions for.
 */
fun chatMessage(block: ChatMessageBuilder.() -> Unit): ChatMessage =
    ChatMessageBuilder().apply(block).build()

/**
 * Builder of [ChatMessageBuilder] instances.
 */
class ChatMessageBuilder {

    /**
     * The role of the message author.
     */
    var role: ChatRole? = null

    /**
     * The contents of the message.
     */
    var content: String? = null


    /**
     * Create [ChatMessageBuilder] instance.
     */
    fun build(): ChatMessage = ChatMessage(
        role = requireNotNull(role) { "role is required" },
        content = requireNotNull(content) { "content is required" },
    )
}
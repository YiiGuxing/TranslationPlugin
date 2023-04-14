package cn.yiiguxing.plugin.translate.trans.openai.chat

import com.google.gson.annotations.SerializedName

/**
 * Creates a completion for the chat message.
 */
data class ChatCompletionRequest(
    /**
     * The model to use.
     */
    @SerializedName("model") val model: String,

    /**
     * The messages to generate chat completions for.
     */
    @SerializedName("messages") val messages: List<ChatMessage>,
)

/**
 * The messages to generate chat completions for.
 */
fun chatCompletionRequest(block: ChatCompletionRequestBuilder.() -> Unit): ChatCompletionRequest =
    ChatCompletionRequestBuilder().apply(block).build()

/**
 * Creates a completion for the chat message.
 */
class ChatCompletionRequestBuilder {
    /**
     * The model to use.
     */
    var model: String? = null

    /**
     * The messages to generate chat completions for.
     */
    var messages: List<ChatMessage>? = null

    /**
     * The messages to generate chat completions for.
     */
    fun messages(block: ChatMessagesBuilder.() -> Unit) {
        messages = ChatMessagesBuilder().apply(block).messages
    }

    /**
     * Builder of [ChatCompletionRequest] instances.
     */
    fun build(): ChatCompletionRequest = ChatCompletionRequest(
        model = requireNotNull(model) { "model is required" },
        messages = requireNotNull(messages) { "messages is required" },
    )
}

/**
 * Creates a list of [ChatMessage].
 */
class ChatMessagesBuilder {
    internal val messages = mutableListOf<ChatMessage>()

    /**
     * Creates a [ChatMessage] instance.
     */
    fun message(block: ChatMessageBuilder.() -> Unit) {
        messages += ChatMessageBuilder().apply(block).build()
    }
}

package cn.yiiguxing.plugin.translate.trans.openai.chat

import com.google.gson.annotations.SerializedName

/**
 * Creates a completion for the chat message.
 */
data class ChatCompletionRequest(
    /**
     * The model to use.
     */
    @SerializedName("model") val model: String?,

    /**
     * The messages to generate chat completions for.
     */
    @SerializedName("messages") val messages: List<ChatMessage>,
)

/**
 * The messages to generate chat completions for.
 */
fun chatCompletionRequest(
    requireModel: Boolean = true,
    block: ChatCompletionRequestBuilder.() -> Unit
): ChatCompletionRequest =
    ChatCompletionRequestBuilder(requireModel).apply(block).build()

/**
 * Creates a completion for the chat message.
 */
class ChatCompletionRequestBuilder(private val requireModel: Boolean = true) {
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
        messages = chatMessages(block)
    }

    /**
     * Builder of [ChatCompletionRequest] instances.
     */
    fun build(): ChatCompletionRequest = ChatCompletionRequest(
        model = if (requireModel) requireNotNull(model) { "model is required" } else model,
        messages = requireNotNull(messages) { "messages is required" },
    )
}

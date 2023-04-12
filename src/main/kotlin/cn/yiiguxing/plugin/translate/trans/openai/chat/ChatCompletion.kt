package cn.yiiguxing.plugin.translate.trans.openai.chat

import com.google.gson.annotations.SerializedName

/**
 * An object containing a response from the chat completion api.
 *
 * [documentation](https://platform.openai.com/docs/api-reference/chat/create)
 */
data class ChatCompletion internal constructor(
    /**
     * A unique id assigned to this completion
     */
    @SerializedName("id") val id: String,

    /**
     * The creation time in epoch milliseconds.
     */
    @SerializedName("created") val created: Int,

    /**
     * The model used.
     */
    @SerializedName("model") val model: String,

    /**
     * A list of generated completions
     */
    @SerializedName("choices") val choices: List<ChatChoice>
)

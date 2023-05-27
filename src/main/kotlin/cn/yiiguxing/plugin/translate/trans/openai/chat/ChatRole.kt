@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai.chat

import com.google.gson.annotations.SerializedName

/**
 * The role of the message author.
 *
 * [documentation](https://platform.openai.com/docs/api-reference/chat/create)
 */
enum class ChatRole(val role: String) {
    @SerializedName("system")
    SYSTEM("system"),

    @SerializedName("user")
    USER("user"),

    @SerializedName("assistant")
    ASSISTANT("assistant");
}

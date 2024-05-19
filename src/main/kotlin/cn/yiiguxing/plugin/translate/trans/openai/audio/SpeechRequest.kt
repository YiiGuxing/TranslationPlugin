package cn.yiiguxing.plugin.translate.trans.openai.audio

import com.google.gson.annotations.SerializedName

/**
 * [Documentation](https://platform.openai.com/docs/api-reference/audio/createSpeech)
 */
data class SpeechRequest(
    @SerializedName("model") val module: String,
    @SerializedName("input") val input: String,
    @SerializedName("voice") val voice: String,
    @SerializedName("speed") val speed: Float,
    @SerializedName("response_format") val responseFormat: String = "mp3",
)
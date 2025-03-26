@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.trans.openai

/**
 * See: [OpenAI TTS voice](https://platform.openai.com/docs/guides/text-to-speech/voice-options).
 */
enum class OpenAiTtsVoice(val value: String, val voiceName: String) {
    ALLOY("alloy", "Alloy"),
    ASH("ash", "Ash"),
    BALLAD("ballad", "Ballad"),
    CORAL("coral", "Coral"),
    ECHO("echo", "Echo"),
    FABLE("fable", "Fable"),
    ONYX("onyx", "Onyx"),
    NOVA("nova", "Nova"),
    SAGE("sage", "Sage"),
    SHIMMER("shimmer", "Shimmer"),
}
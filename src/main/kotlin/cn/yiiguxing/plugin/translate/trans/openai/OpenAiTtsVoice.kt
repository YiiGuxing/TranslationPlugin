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
    NOVA("nova", "Nova"),
    ONYX("onyx", "Onyx"),
    SAGE("sage", "Sage"),
    SHIMMER("shimmer", "Shimmer"),
    VERSE("verse", "Verse"),
    MARIN("marin", "Marin"),
    CEDAR("cedar", "Cedar");

    companion object {
        private val unsupportedForClassic = setOf(
            BALLAD,
            VERSE,
            MARIN,
            CEDAR
        )

        /**
         * Returns a list of voices that are supported by the classic TTS models (e.g. tts-1 and tts-1-hd).
         */
        fun getClassicVoices(): List<OpenAiTtsVoice> {
            return values().filter { it !in unsupportedForClassic }
        }
    }
}
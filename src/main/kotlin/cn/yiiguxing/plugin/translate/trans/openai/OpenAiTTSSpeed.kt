package cn.yiiguxing.plugin.translate.trans.openai

object OpenAiTTSSpeed {
    const val MIN = 0
    const val MAX = 400
    const val NORMAL = 100
    private const val MIN_SPEED = 0.25f

    fun get(value: Int): Float {
        val coerced = value.coerceIn(MIN, MAX)
        if (coerced >= NORMAL) {
            return coerced.toFloat() / NORMAL
        }

        return MIN_SPEED + (coerced.toFloat() / NORMAL) * (1 - MIN_SPEED)
    }
}

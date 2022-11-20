package cn.yiiguxing.plugin.translate.trans.microsoft

data class MicrosoftTranslation(
    val detectedLanguage: MSDetectedLanguage? = null,
    val translations: List<MSTranslation> = emptyList()
)

data class MSDetectedLanguage(val language: String, val score: Float)
data class MSTranslation(val to: String, val text: String)

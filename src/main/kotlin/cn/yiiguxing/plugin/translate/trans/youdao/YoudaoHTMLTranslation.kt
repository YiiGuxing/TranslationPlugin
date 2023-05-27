package cn.yiiguxing.plugin.translate.trans.youdao

import com.google.gson.annotations.SerializedName

data class YoudaoHTMLTranslation(
    @SerializedName("data")
    val data: YoudaoHTMLTranslationData? = null,
    @SerializedName("errorCode")
    val errorCode: Int = -1,
    @SerializedName("errorMessage")
    val errorMessage: String? = null,
)

data class YoudaoHTMLTranslationData(@SerializedName("translation") val translation: String)
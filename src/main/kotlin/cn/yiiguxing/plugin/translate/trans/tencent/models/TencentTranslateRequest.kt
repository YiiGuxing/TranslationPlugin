package cn.yiiguxing.plugin.translate.trans.tencent.models

import com.google.gson.annotations.SerializedName

/**
 * Tencent translate request model
 */
data class TencentTranslateRequest(
    @SerializedName("SourceText")
    val sourceText: String,
    @SerializedName("Source")
    val source: String,
    @SerializedName("Target")
    val target: String,
    @SerializedName("ProjectId")
    val projectId: Int = 0,
    @SerializedName("UntranslatedText")
    val untranslatedText: String? = null
)
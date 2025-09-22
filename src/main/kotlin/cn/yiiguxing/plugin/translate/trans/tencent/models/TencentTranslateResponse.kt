package cn.yiiguxing.plugin.translate.trans.tencent.models

import com.google.gson.annotations.SerializedName

/**
 * Tencent translate response model
 */
internal data class TencentTranslateResponse(
    @SerializedName("Response")
    val response: TencentTranslateResult
)

internal data class TencentTranslateResult(
    @SerializedName("TargetText")
    val targetText: String,
    @SerializedName("Source")
    val source: String,
    @SerializedName("Target")
    val target: String,
    @SerializedName("UsedAmount")
    val usedAmount: Int? = null,
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Error")
    val error: TencentError? = null
)

internal data class TencentError(
    @SerializedName("Code")
    val code: String,
    @SerializedName("Message")
    val message: String
)
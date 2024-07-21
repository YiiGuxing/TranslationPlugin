package cn.yiiguxing.plugin.translate.trans.ali.models

import com.google.gson.annotations.SerializedName

internal data class AliMTResponse<Data>(
    @SerializedName("RequestId")
    val requestId: String,
    @SerializedName("Code")
    val code: String = "-1",
    @SerializedName("Data")
    val data: Data? = null,
    @SerializedName("Message")
    val message: String? = null,
) {
    val isSuccessful get() = code == "200"
}
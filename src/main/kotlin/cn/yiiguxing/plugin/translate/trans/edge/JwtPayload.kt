package cn.yiiguxing.plugin.translate.trans.edge

import com.google.gson.annotations.SerializedName

data class JwtPayload(
    @SerializedName("aud")
    val aud: String,
    @SerializedName("azure-resource-id")
    val azureResourceId: String,
    @SerializedName("cognitive-services-endpoint")
    val cognitiveServicesEndpoint: String,
    @SerializedName("exp")
    val exp: Int,
    @SerializedName("iss")
    val iss: String,
    @SerializedName("product-id")
    val productId: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("scope")
    val scope: String,
    @SerializedName("subscription-id")
    val subscriptionId: String
)
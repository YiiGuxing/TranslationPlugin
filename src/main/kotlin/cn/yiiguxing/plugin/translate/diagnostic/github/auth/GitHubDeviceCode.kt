package cn.yiiguxing.plugin.translate.diagnostic.github.auth

import com.google.gson.annotations.SerializedName

internal data class GitHubDeviceCode(
    @SerializedName("device_code")
    val code: String = "",
    @SerializedName("user_code")
    val userCode: String = "",
    @SerializedName("verification_uri")
    val verificationUri: String = "",
    @SerializedName("expires_in")
    val expiresIn: Int = 0,
    @SerializedName("interval")
    val interval: Int = 0
) {

    // 上面的字段需要设置默认值，否则在GSON创建对象时不会对此事字段正确赋值。
    private val timestamp: Long = System.currentTimeMillis()

    val expiresTimestamp: Long get() = timestamp + expiresIn * 1000

}

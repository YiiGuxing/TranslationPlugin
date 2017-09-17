package cn.yiiguxing.plugin.translate.model

import cn.yiiguxing.plugin.translate.LINK_SETTINGS
import com.google.gson.annotations.SerializedName
import java.util.*

data class QueryResult(
        @SerializedName("query")
        var query: String? = null,
        @SerializedName("errorCode")
        var errorCode: Int = 0,
        var message: String? = null,
        @SerializedName("translation")
        var translation: Array<String>? = null,
        @SerializedName("basic")
        var basicExplain: BasicExplain? = null,
        @SerializedName("web")
        var webExplains: Array<WebExplain>? = null
) {

    val isSuccessful: Boolean
        get() = errorCode == 0

    fun checkError() {
        if (isSuccessful && translation?.isEmpty() != false && basicExplain?.explains?.isEmpty() != false) {
            errorCode = 302
        }

        if (!isSuccessful && message.isNullOrEmpty()) {
            message = getErrorMessage(errorCode) ?: "未知错误:[$errorCode]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryResult

        if (query != other.query) return false
        if (errorCode != other.errorCode) return false
        if (!Arrays.equals(translation, other.translation)) return false
        if (message != other.message) return false
        if (basicExplain != other.basicExplain) return false
        if (!Arrays.equals(webExplains, other.webExplains)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = query?.hashCode() ?: 0
        result = 31 * result + errorCode
        result = 31 * result + (translation?.let { Arrays.hashCode(it) } ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (basicExplain?.hashCode() ?: 0)
        result = 31 * result + (webExplains?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }

    companion object {

        const val CODE_ERROR = -1
        const val CODE_JSON_SYNTAX_ERROR = -2

        fun getErrorMessage(errorCode: Int): String? = when (errorCode) {
            101 -> "缺少必填的参数"
            102 -> "不支持的语言类型"
            103 -> "翻译文本过长"
            104 -> "不支持的API类型"
            105 -> "不支持的签名类型"
            106 -> "不支持的响应类型"
            107 -> "不支持的传输加密类型"
            108 -> "AppKey无效 - $LINK_SETTINGS"
            109 -> "BatchLog格式不正确"
            110 -> "无相关服务的有效实例"
            111 -> "账号无效或者账号已欠费 - $LINK_SETTINGS"
            201 -> "解密失败"
            202 -> "签名检验失败 - $LINK_SETTINGS"
            203 -> "访问IP地址不在可访问IP列表"
            301 -> "辞典查询失败"
            302 -> "翻译查询失败"
            303 -> "服务器异常"
            401 -> "账户已经欠费"
            else -> null
        }
    }

}

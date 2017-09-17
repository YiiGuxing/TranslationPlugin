package cn.yiiguxing.plugin.translate.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class WebExplain(
        @SerializedName(value = "key")
        var key: String? = null,
        @SerializedName(value = "value")
        var values: Array<String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebExplain

        if (key != other.key) return false
        if (!Arrays.equals(values, other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + (values?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}

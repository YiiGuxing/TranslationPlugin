package cn.yiiguxing.plugin.translate.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class BasicExplain(
        @SerializedName(value = "phonetic")
        var phonetic: String? = null,
        @SerializedName(value = "uk-phonetic")
        var phoneticUK: String? = null,
        @SerializedName(value = "us-phonetic")
        var phoneticUS: String? = null,
        @SerializedName(value = "explains")
        var explains: Array<String>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BasicExplain

        if (phonetic != other.phonetic) return false
        if (phoneticUK != other.phoneticUK) return false
        if (phoneticUS != other.phoneticUS) return false
        if (!Arrays.equals(explains, other.explains)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = phonetic?.hashCode() ?: 0
        result = 31 * result + (phoneticUK?.hashCode() ?: 0)
        result = 31 * result + (phoneticUS?.hashCode() ?: 0)
        result = 31 * result + (explains?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}

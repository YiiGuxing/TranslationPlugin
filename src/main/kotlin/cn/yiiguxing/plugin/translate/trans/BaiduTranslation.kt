/*
 * BaiduTranslation
 * 
 * Created by Yii.Guxing on 2018/04/19.
 */

package cn.yiiguxing.plugin.translate.trans

import com.google.gson.annotations.SerializedName


@Suppress("MemberVisibilityCanBePrivate")
data class BaiduTranslation(
        @SerializedName("error_code")
        val code: Int = 0,
        @SerializedName("from")
        val srcLanguage: String? = null,
        @SerializedName("to")
        val targetLanguage: String? = null,
        @SerializedName("trans_result")
        val trans: List<BTrans> = emptyList()
) : TranslationAdapter {

    val isSuccessful get() = code == 0 || code == 52000

    override fun toTranslation(): Translation {
        check(isSuccessful) { "Can not convert to Translation: errorCode=$code" }
        check(srcLanguage != null) { "Can not convert to Translation: srcLanguage=null" }
        check(targetLanguage != null) { "Can not convert to Translation: targetLanguage=null" }
        check(trans.isNotEmpty()) { "Can not convert to Translation: trans=[]" }

        val srcLang = Lang.valueOfBaiduCode(srcLanguage!!)
        val transLang = Lang.valueOfBaiduCode(targetLanguage!!)
        val translation = trans.first()

        return Translation(translation.src, translation.dst, srcLang, transLang, listOf(srcLang))
    }
}

data class BTrans(
        @SerializedName("src")
        val src: String,
        @SerializedName("dst")
        val dst: String)

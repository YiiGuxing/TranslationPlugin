/*
 * Strings
 * 
 * Created by Yii.Guxing on 2017/9/11
 */
package cn.yiiguxing.plugin.translate.util


/**
 * 单词拆分
 */
fun String?.splitWord(): String? = if (this == null || this.isBlank()) this else
    replace("[_*\\s]+".toRegex(), " ")
            .replace("([A-Z][a-z]+)|([0-9\\W]+)".toRegex(), " $0 ")
            .replace("[A-Z]{2,}".toRegex(), " $0")
            .replace("\\s{2,}".toRegex(), " ")
            .trim()
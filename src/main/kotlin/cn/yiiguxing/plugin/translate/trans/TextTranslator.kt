package cn.yiiguxing.plugin.translate.trans

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

interface TextTranslator {

    @RequiresBackgroundThread
    fun translate(text: String, srcLang: Lang, targetLang: Lang): Translation

}
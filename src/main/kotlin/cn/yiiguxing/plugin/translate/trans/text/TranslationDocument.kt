package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer

interface TranslationDocument {

    val translations: Set<String>

    fun setupTo(viewer: StyledViewer)

    interface Parser<T, R : TranslationDocument> {
        fun parse(input: T): R
    }

}

fun StyledViewer.setup(document: TranslationDocument) = document.setupTo(this)
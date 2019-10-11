package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer

interface TranslationDocument {

    fun setupTo(viewer: StyledViewer)

    interface Parser<T> {
        fun parse(input: T): TranslationDocument
    }

}

fun StyledViewer.setup(document: TranslationDocument) = document.setupTo(this)
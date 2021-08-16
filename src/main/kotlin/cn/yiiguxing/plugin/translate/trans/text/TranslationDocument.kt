package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer

interface TranslationDocument {

    val translations: Set<String> get() = emptySet()

    val text: String

    fun applyTo(viewer: StyledViewer)

    interface Factory<T, R : TranslationDocument> {
        fun getDocument(input: T): R?
    }
}

fun StyledViewer.apply(document: TranslationDocument?) = document?.applyTo(this)
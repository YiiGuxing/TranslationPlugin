package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer

interface TranslationDocument {

    val translations: Set<String> get() = emptySet()

    val text: String

    fun setupTo(viewer: StyledViewer)

    interface Factory<T, R : TranslationDocument> {
        fun getDocument(input: T): R?
    }
}

class NamedTranslationDocument(val name: String, document: TranslationDocument) : TranslationDocument by document

fun StyledViewer.setup(document: TranslationDocument) = document.setupTo(this)
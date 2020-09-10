package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.appendString

interface TranslationDocument {

    val translations: Set<String> get() = emptySet()

    val text: String

    fun setupTo(viewer: StyledViewer)

    interface Factory<T, R : TranslationDocument> {
        fun getDocument(input: T): R?
    }
}

class NamedTranslationDocument(val name: String, private val document: TranslationDocument) : TranslationDocument by document {
    //todo: nicer style?
    fun appendName(viewer: StyledViewer) {
        viewer.styledDocument.appendString("\n\n$name\n\n")
    }

    override fun setupTo(viewer: StyledViewer) {
        appendName(viewer)
        document.setupTo(viewer)
    }
}

fun StyledViewer.setup(document: TranslationDocument?) = document?.setupTo(this)
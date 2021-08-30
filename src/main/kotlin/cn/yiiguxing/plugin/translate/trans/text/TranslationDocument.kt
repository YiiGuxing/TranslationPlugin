package cn.yiiguxing.plugin.translate.trans.text

import cn.yiiguxing.plugin.translate.ui.StyledViewer
import cn.yiiguxing.plugin.translate.util.text.clear
import cn.yiiguxing.plugin.translate.util.text.newLine

interface TranslationDocument {

    val translations: Set<String> get() = emptySet()

    val text: String

    fun applyTo(viewer: StyledViewer)

    interface Factory<T, R : TranslationDocument> {
        fun getDocument(input: T): R?
    }
}

fun StyledViewer.apply(doc: TranslationDocument) {
    document.clear()
    doc.applyTo(this)
}

fun StyledViewer.append(doc: TranslationDocument, newLine: Boolean = true) {
    val document = document
    if (newLine && document.length > 0 && document.getText(document.length - 1, 1) != "\n") {
        document.newLine()
    }
    doc.applyTo(this)
}
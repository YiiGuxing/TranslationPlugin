/**
 * Texts
 * <p>
 * Created by Yii.Guxing on 2017-09-17 0017.
 */
package cn.yiiguxing.plugin.translate.util

import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.Style
import javax.swing.text.StyledDocument

fun Document.appendString(str: String, attr: AttributeSet? = null) = apply { insertString(length, str, attr) }

fun Document.clear() = apply {
    if (length > 0) remove(0, length)
}

fun Document.trimEnd(predicate: (Char) -> Boolean = Char::isWhitespace) = apply {
    var length = this.length
    while (length > 0 && predicate(getText(--length, 1)[0])) {
        remove(length, 1)
    }
}

inline fun StyledDocument.addStyle(name: String, parent: Style? = null, init: (Style) -> Unit): Style =
        addStyle(name, parent).apply(init)
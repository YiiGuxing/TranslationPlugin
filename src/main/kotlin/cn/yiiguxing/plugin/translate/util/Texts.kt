/**
 * Texts
 * <p>
 * Created by Yii.Guxing on 2017-09-17 0017.
 */
package cn.yiiguxing.plugin.translate.util

import javax.swing.text.AttributeSet
import javax.swing.text.Document

fun Document.appendString(str: String, attr: AttributeSet? = null) = insertString(length, str, attr)

fun Document.clear() {
    if (length > 0) remove(0, length)
}

fun Document.trimEnd() {
    var length = this.length
    while (length > 0 && getText(--length, 1)[0] <= ' ') {
        remove(length, 1)
    }
}
/**
 * Documents
 * <p>
 * Created by Yii.Guxing on 2017-09-17 0017.
 */
@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.util

import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.Style
import javax.swing.text.StyledDocument

/**
 * 在指定的偏移量([offset])插入字符串([str])
 *
 * @return 插入后`offset`的位置
 * @see [Document.insertString]
 */
fun Document.insert(offset: Int, str: String, attr: AttributeSet? = null): Int {
    insertString(offset, str, attr)
    return offset + str.length
}

/**
 * 在[Document]的末尾插入字符串([str])
 *
 * @see [Document.insertString]
 */
fun Document.appendString(str: String, attr: AttributeSet? = null) = apply { insertString(length, str, attr) }

/**
 * 清空[Document]内容
 */
fun Document.clear() = apply {
    if (length > 0) remove(0, length)
}

/**
 * Removes trailing whitespace
 */
fun Document.trimEnd(predicate: (Char) -> Boolean = Char::isWhitespace) = apply {
    var length = this.length
    while (length > 0 && predicate(getText(--length, 1)[0])) {
        remove(length, 1)
    }
}

/**
 * 添加样式
 */
inline fun StyledDocument.addStyle(name: String, parent: Style? = null, init: Style.() -> Unit = {}): Style =
        addStyle(name, parent).apply(init)
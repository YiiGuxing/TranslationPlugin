/**
 * Documents
 */

package cn.yiiguxing.plugin.translate.util.text

import javax.swing.text.AttributeSet
import javax.swing.text.Document
import javax.swing.text.Style
import javax.swing.text.StyledDocument

/**
 * 在指定的偏移量([offset])插入字符串([str])
 *
 * @return 插入后`offset`的位置
 * @see Document.insertString
 */
fun Document.insert(offset: Int, str: String, attr: AttributeSet? = null): Int {
    insertString(offset, str, attr)
    return offset + str.length
}

fun StyledDocument.insert(offset: Int, str: String, style: String): Int = insert(offset, str, getStyle(style))

/**
 * 将字符串([str])替换指定范围内的内容并返回替换后的偏移量，被替换的范围从偏移量[offset]开始，长度为[length]
 */
fun Document.replace(offset: Int, length: Int, str: String, attr: AttributeSet? = null): Int {
    if (length > 0) {
        remove(offset, length)
    }
    return insert(offset, str, attr)
}

/**
 * 在[Document]的末尾插入字符串([str])
 *
 * @see Document.insertString
 */
fun Document.appendString(str: String, attr: AttributeSet? = null) = apply { insertString(length, str, attr) }

fun Document.newLine() = appendString("\n")

fun StyledDocument.appendString(str: String, style: String) = apply { appendString(str, getStyle(style)) }

fun StyledDocument.appendCharSequence(charSequence: CharSequence) = apply {
    if (charSequence is StyledString) {
        appendString(charSequence.toString(), charSequence.style)
    } else {
        appendString(charSequence.toString())
    }
}

/**
 * 清空[Document]内容
 */
fun Document.clear() = apply {
    if (length > 0) remove(0, length)
}

/**
 * 添加样式
 */
inline fun StyledDocument.getStyleOrAdd(name: String, parent: Style? = null, init: (style: Style) -> Unit = {}): Style {
    return getStyle(name) ?: addStyle(name, parent).also(init)
}

fun StyledDocument.setParagraphStyle(
    offset: Int? = null,
    len: Int = 0,
    style: String,
    replace: Boolean = true
) {
    setParagraphAttributes(offset ?: length, len, getStyle(style), replace)
}
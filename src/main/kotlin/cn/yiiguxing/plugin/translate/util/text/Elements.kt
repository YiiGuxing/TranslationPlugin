/*
 * Elements
 */

package cn.yiiguxing.plugin.translate.util.text

import javax.swing.text.Element

inline val Element.rangeSize: Int
    get() = endOffset - startOffset

inline val Element.text: String
    get() = document.getText(startOffset, rangeSize)
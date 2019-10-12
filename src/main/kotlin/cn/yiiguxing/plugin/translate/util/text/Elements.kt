/*
 * Elements
 * 
 * Created by Yii.Guxing on 2019/10/12.
 */

package cn.yiiguxing.plugin.translate.util.text

import javax.swing.text.Element

inline val Element.rangeSize: Int
    get() = endOffset - startOffset

inline val Element.text: String
    get() = document.getText(startOffset, rangeSize)
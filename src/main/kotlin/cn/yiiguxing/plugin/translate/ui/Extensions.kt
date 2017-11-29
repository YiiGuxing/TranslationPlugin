/*
 * Extensions
 * 
 * Created by Yii.Guxing on 2017/11/20
 */
package cn.yiiguxing.plugin.translate.ui

import sun.swing.SwingUtilities2
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.text.JTextComponent


/**
 * 当前选中项
 */
val <E> JComboBox<E>.selected: E get() = getItemAt(selectedIndex)

/**
 * 自适应大小
 *
 * @param maxWidth 最大宽度
 */
fun JTextComponent.adjustSize(maxWidth: Int = Int.MAX_VALUE) {
    require(maxWidth >= 0) { "maxWidth must be greater than or equal to 0." }

    val minSize = if (isMinimumSizeSet) minimumSize else Dimension(0, 0)
    minimumSize = null
    size = Dimension(maxWidth, Int.MAX_VALUE)

    val textWidth = SwingUtilities2.stringWidth(this, getFontMetrics(font), text)
    val preSize = Dimension(preferredSize).apply {
        width = minOf(width, textWidth)
    }

    size = Dimension(preSize)
    minimumSize = minSize
    preferredSize = preSize
}
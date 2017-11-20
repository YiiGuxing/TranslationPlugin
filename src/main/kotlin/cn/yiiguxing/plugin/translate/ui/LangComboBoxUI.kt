package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import sun.swing.DefaultLookup
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI

/**
 * LangComboBoxUI
 *
 * Created by Yii.Guxing on 2017/11/20
 */
class LangComboBoxUI(
        private val combo: ComboBox<Lang>,
        horizontalAlignment: Int = SwingConstants.LEFT
) : BasicComboBoxUI() {

    private val label: JLabel
    private val arrowIcon: ComboArrowIcon

    init {
        combo.border = BorderFactory.createEmptyBorder()
        combo.isEditable = false

        arrowIcon = ComboArrowIcon()
        label = JLabel(arrowIcon).apply {
            horizontalTextPosition = SwingConstants.LEFT
            setHorizontalAlignment(horizontalAlignment)
        }
    }

    override fun installDefaults() {
        super.installDefaults()

        if (combo !== comboBox) {
            throw IllegalStateException("Not expected UI.")
        }
    }

    override fun createArrowButton(): JButton? = null

    override fun getSizeForComponent(comp: Component): Dimension =
            super.getSizeForComponent(comp).apply { width += JBUI.scale(10) }

    override fun getMinimumSize(c: JComponent?): Dimension {
        if (!isMinimumSizeDirty) {
            return Dimension(cachedMinimumSize)
        }

        val size = displaySize
        val insets = insets
        size.height += insets.top + insets.bottom
        size.width += insets.left + insets.right + label.iconTextGap + arrowIcon.iconWidth

        cachedMinimumSize.setSize(size.width, size.height)
        isMinimumSizeDirty = false

        return Dimension(size)
    }

    override fun paintCurrentValueBackground(g: Graphics, bounds: Rectangle, hasFocus: Boolean) = Unit

    override fun paintCurrentValue(g: Graphics, bounds: Rectangle, hasFocus: Boolean) {
        with(combo) {
            label.font = font
            val foregroundColor = if (isEnabled) {
                foreground
            } else {
                DefaultLookup.getColor(this, this@LangComboBoxUI, "ComboBox.disabledForeground", null)
            }

            label.foreground = foregroundColor
            arrowIcon.color = foregroundColor

            label.font = font
            label.text = selected.langName
        }

        var x = bounds.x
        var y = bounds.y
        var w = bounds.width
        var h = bounds.height

        padding?.let {
            x = bounds.x + it.left
            y = bounds.y + it.top
            w = bounds.width - (it.left + it.right)
            h = bounds.height - (it.top + it.bottom)
        }

        currentValuePane.paintComponent(g, label, combo, x, y, w, h, false)
    }

    override fun rectangleForCurrentValue(): Rectangle {
        val width = combo.width
        val height = combo.height
        val insets = insets
        return if (combo.componentOrientation.isLeftToRight) {
            Rectangle(insets.left, insets.top,
                    width - (insets.left + insets.right),
                    height - (insets.top + insets.bottom))
        } else {
            Rectangle(insets.left, insets.top,
                    width - (insets.left + insets.right),
                    height - (insets.top + insets.bottom))
        }
    }

}
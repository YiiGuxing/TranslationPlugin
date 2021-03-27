package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.ui.icon.ComboArrowIcon
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI

/**
 * LangComboBoxUI
 */
class LangComboBoxUI(
    private val myComboBox: ComboBox<Lang>,
    horizontalAlignment: Int = SwingConstants.LEFT
) : BasicComboBoxUI() {

    private val label: JLabel
    private val arrowIcon: ComboArrowIcon

    init {
        myComboBox.apply {
            border = BorderFactory.createEmptyBorder()
            renderer = SimpleListCellRenderer.create<Lang> { label, value, _ ->
                label.text = value.langName
                label.font = this@apply.font
            }
            isEditable = false
        }

        arrowIcon = ComboArrowIcon()
        label = JLabel(arrowIcon).apply {
            horizontalTextPosition = SwingConstants.LEFT
            setHorizontalAlignment(horizontalAlignment)
        }
    }

    override fun installDefaults() {
        super.installDefaults()

        if (myComboBox !== comboBox) {
            throw IllegalStateException("Not expected component.")
        }
    }

    override fun createArrowButton(): JButton? = null

    override fun getSizeForComponent(comp: Component): Dimension =
        super.getSizeForComponent(comp).apply { width += JBUI.scale(10) }

    override fun getMinimumSize(c: JComponent): Dimension {
        if (!isMinimumSizeDirty) {
            return Dimension(cachedMinimumSize)
        }

        return displaySize.let {
            JBInsets.addTo(it, insets)
            it.width += label.iconTextGap + arrowIcon.iconWidth

            cachedMinimumSize.size = it
            isMinimumSizeDirty = false

            Dimension(it)
        }
    }

    override fun paintCurrentValueBackground(g: Graphics, bounds: Rectangle, hasFocus: Boolean) = Unit

    override fun paintCurrentValue(g: Graphics, bounds: Rectangle, hasFocus: Boolean) {
        with(myComboBox) {
            val foregroundColor = if (isEnabled) {
                foreground
            } else {
                UIManager.getColor("ComboBox.disabledForeground")
            }

            label.foreground = foregroundColor
            arrowIcon.color = foregroundColor

            label.font = font
            label.text = selected?.langName
        }

        currentValuePane.paintComponent(g, label, myComboBox,
            Rectangle(bounds).also { JBInsets.removeFrom(it, padding) })
    }

    override fun rectangleForCurrentValue(): Rectangle = Rectangle(comboBox.width, comboBox.height).apply {
        JBInsets.removeFrom(this, insets)
    }

}

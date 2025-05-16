package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.layout.LayoutUtil
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.border.Border
import javax.swing.text.JTextComponent

/**
 * UI
 */
object UI {

    // 使用`get() = ...`以保证获得实时`ScaledFont`
    val defaultFont: JBFont get() = JBFont.create(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL))

    @JvmStatic
    private fun getFont(fontFamily: String?, size: Int): JBFont = if (!fontFamily.isNullOrBlank()) {
        JBUI.Fonts.create(fontFamily, size)
    } else {
        defaultFont.deriveScaledFont(size.toFloat())
    }

    @JvmStatic
    fun getFonts(primaryFontSize: Int, phoneticFontSize: Int): FontPair {
        val settings = Settings.getInstance()
        return FontPair(
            getFont(settings.primaryFontFamily, primaryFontSize),
            getFont(settings.phoneticFontFamily, phoneticFontSize)
        )
    }

    data class FontPair(val primary: JBFont, val phonetic: JBFont)

    @JvmStatic
    fun Icon.disabled(): Icon = IconLoader.getDisabledIcon(this)

    @JvmStatic
    fun getBorderColor(): Color = JBUI.CurrentTheme.Popup.borderColor(true)

    fun <T> LinkLabel<T>.setIcons(baseIcon: Icon) {
        icon = baseIcon
        disabledIcon = IconLoader.getDisabledIcon(baseIcon)
        setHoveringIcon(IconUtil.darker(baseIcon, 3))
    }

    fun migLayout(gapX: String = "0!", gapY: String = "0!", insets: String = "0", lcBuilder: (LC.() -> Unit)? = null) =
        MigLayout(LC().fill().gridGap(gapX, gapY).insets(insets).also { lcBuilder?.invoke(it) })

    fun migLayoutVertical() =
        MigLayout(LC().flowY().fill().gridGap("0!", "0!").insets("0"))

    /**
     * In different IDE versions, the default unit of MigLayout is different.
     * Therefore, it is necessary to use this method to unify the unit as pixels.
     */
    fun migSize(size: Int, scale: Boolean = true): String = "${if (scale) JBUIScale.scale(size) else size}px"

    fun migInsets(top: Int = 0, left: Int = 0, bottom: Int = 0, right: Int = 0, scale: Boolean = true): String =
        "${migSize(top, scale)} ${migSize(left, scale)} ${migSize(bottom, scale)} ${migSize(right, scale)}"

    fun cc() = CC()

    fun spanX(cells: Int = LayoutUtil.INF): CC = CC().spanX(cells)

    fun fill(): CC = CC().grow().push()
    fun fillX(): CC = CC().growX().pushX()
    fun fillY(): CC = CC().growY().pushY()

    fun wrap(): CC = CC().wrap()

    fun emptyBorder(topAndBottom: Int, leftAndRight: Int) = JBUI.Borders.empty(topAndBottom, leftAndRight)

    fun emptyBorder(offsets: Int) = JBUI.Borders.empty(offsets)

    fun lineAbove(): Border = JBUI.Borders.customLine(getBorderColor(), 1, 0, 0, 0)

    fun lineBelow(): Border = JBUI.Borders.customLine(getBorderColor(), 0, 0, 1, 0)

    @Suppress("unused")
    fun lineToRight(): Border = JBUI.Borders.customLine(getBorderColor(), 0, 0, 0, 1)

    operator fun Border.plus(external: Border): Border = JBUI.Borders.merge(this, external, true)

    fun createHint(content: String, componentWidth: Int = 300, hintForComponent: JComponent? = null): JTextComponent =
        JEditorPane().apply {
            isEditable = false
            isFocusable = false
            isOpaque = false
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = JBFont.label().lessOn(1f)
            editorKit = HTMLEditorKitBuilder.simple()
            border = hintForComponent?.insets.let {
                JBUI.Borders.empty(2, it?.left ?: 0, 0, it?.right ?: 0)
            }
            UIUtil.enableEagerSoftWrapping(this)
            text = content

            val scaledComponentWidth = componentWidth.scaled
            /* Arbitrary large height, that doesn't lead to overflows and precision loss */
            setSize(scaledComponentWidth, 10000000)
            // trigger internal layout and reset preferred size
            preferredSize = Dimension(scaledComponentWidth, preferredSize.height)

            addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
        }
}
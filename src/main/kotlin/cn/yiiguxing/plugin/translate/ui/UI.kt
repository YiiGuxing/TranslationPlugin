package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.layout.LayoutUtil
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.image.RGBImageFilter
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.border.Border

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
        return FontPair(
            getFont(Settings.primaryFontFamily, primaryFontSize),
            getFont(Settings.phoneticFontFamily, phoneticFontSize)
        )
    }

    data class FontPair(val primary: JBFont, val phonetic: JBFont)

    @JvmStatic
    fun Icon.disabled(): Icon = IconUtil.filterIcon(this, { DisabledFilter() }, null)

    private class DisabledFilter(color: Color = JBUI.CurrentTheme.Label.disabledForeground()) : RGBImageFilter() {
        private val rgb = color.rgb

        override fun filterRGB(x: Int, y: Int, argb: Int): Int {
            return argb and -0x1000000 or (rgb and 0x00ffffff)
        }
    }

    @JvmStatic
    fun getBorderColor(): Color = JBUI.CurrentTheme.Popup.borderColor(true)

    fun <T> LinkLabel<T>.setIcons(baseIcon: Icon) {
        icon = baseIcon
        disabledIcon = IconLoader.getDisabledIcon(baseIcon)
        setHoveringIcon(IconUtil.darker(baseIcon, 3))
    }

    fun migLayout(gapX: String = "0!", gapY: String = "0!", insets: String = "0") =
        MigLayout(LC().fill().gridGap(gapX, gapY).insets(insets))

    fun migLayoutVertical() =
        MigLayout(LC().flowY().fill().gridGap("0!", "0!").insets("0"))

    /**
     * In different IDE versions, the default unit of MigLayout is different.
     * Therefore, it is necessary to use this method to unify the unit as pixels.
     */
    fun migSize(size: Int, scale: Boolean = true): String = "${if (scale) JBUIScale.scale(size) else size}px"

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

    fun createHint(content: String, componentWidth: Int = 300, hintForComponent: JComponent? = null): JComponent =
        JEditorPane().apply {
            isEditable = false
            isFocusable = false
            isOpaque = false
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = JBFont.label().lessOn(1f)
            editorKit = UIUtil.getHTMLEditorKit()
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
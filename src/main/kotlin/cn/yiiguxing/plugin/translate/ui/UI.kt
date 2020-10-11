package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.image.RGBImageFilter
import javax.swing.Icon
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.text.html.HTMLEditorKit

/**
 * UI
 */
object UI {

    // 使用`get() = ...`以保证获得实时`ScaledFont`
    val defaultFont: JBFont get() = JBFont.create(UIUtil.getLabelFont(UIUtil.FontSize.NORMAL))

    val errorHTMLKit: HTMLEditorKit
        get() = UIUtil.getHTMLEditorKit().apply {
            with(styleSheet) {
                val font = primaryFont(15)
                addRule("body{color:#FF3333;font-family:${font.family};font-size:${font.size}pt;text-align:center;}")
                addRule("a {color:#FF0000;font-weight:bold;text-decoration:none;}")
            }
        }

    fun primaryFont(size: Int)
            : JBFont = getFont(Settings.takeIf { it.isOverrideFont }?.primaryFontFamily, size)

    private fun getFont(fontFamily: String?, size: Int): JBFont = if (!fontFamily.isNullOrEmpty()) {
        JBUI.Fonts.create(fontFamily, size)
    } else {
        defaultFont.deriveScaledFont(size.toFloat())
    }

    @JvmStatic
    fun getFonts(primaryFontSize: Int, phoneticFontSize: Int): FontPair {
        var primaryFont: JBFont? = null
        var phoneticFont: JBFont? = null
        Settings.takeIf { it.isOverrideFont }?.let { settings ->
            primaryFont = settings.primaryFontFamily?.let { JBUI.Fonts.create(it, primaryFontSize) }
            phoneticFont = settings.phoneticFontFamily?.let { JBUI.Fonts.create(it, phoneticFontSize) }
        }

        return FontPair(
            primaryFont ?: defaultFont.deriveScaledFont(primaryFontSize.toFloat()),
            phoneticFont ?: defaultFont.deriveScaledFont(phoneticFontSize.toFloat())
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
    @JvmOverloads
    fun getColor(key: String, default: Color? = null): Color? = UIManager.getColor(key) ?: default

    @JvmStatic
    @JvmOverloads
    fun getBordersColor(default: Color? = null): Color? = UIManager.getColor("Borders.color") ?: default

    fun <T> LinkLabel<T>.setIcons(baseIcon: Icon) {
        icon = baseIcon
        disabledIcon = IconLoader.getDisabledIcon(baseIcon)
        setHoveringIcon(IconUtil.darker(baseIcon, 3))
    }

    fun migLayout() =
        MigLayout(LC().fill().gridGap("0!", "0!").insets("0"))

    fun migLayoutVertical() =
        MigLayout(LC().flowY().fill().gridGap("0!", "0!").insets("0"))

    fun fill(): CC = CC().grow().push()

    fun fillX(): CC = CC().growX().pushX()
    fun fillY(): CC = CC().growY().pushY()

    fun wrap(): CC = CC().wrap()

    fun emptyBorder(topAndBottom: Int, leftAndRight: Int) = JBUI.Borders.empty(topAndBottom, leftAndRight)

    fun emptyBorder(offsets: Int) = JBUI.Borders.empty(offsets)

    fun lineAbove(): Border = JBUI.Borders.customLine(getBordersColor(), 1, 0, 0, 0)

    fun lineBelow(): Border = JBUI.Borders.customLine(getBordersColor(), 0, 0, 1, 0)

    fun lineToRight(): Border = JBUI.Borders.customLine(getBordersColor(), 0, 0, 0, 1)

    operator fun Border.plus(external: Border): Border = JBUI.Borders.merge(this, external, true)
}
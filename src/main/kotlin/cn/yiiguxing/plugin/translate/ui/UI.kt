package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font
import javax.swing.UIManager
import javax.swing.text.html.HTMLEditorKit

/**
 * UI
 */
object UI {

    // 使用`get() = ...`以保证获得实时`ScaledFont`
    val defaultFont: JBFont get() = JBFont.create(Font("Microsoft YaHei", Font.PLAIN, 14))

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
    @JvmOverloads
    fun getColor(key: String, default: Color? = null): Color? = UIManager.getColor(key) ?: default

    @JvmStatic
    @JvmOverloads
    fun getBordersColor(default: Color? = null): Color? = UIManager.getColor("Borders.color") ?: default

}
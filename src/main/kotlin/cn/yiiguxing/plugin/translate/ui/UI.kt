package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Font
import javax.swing.text.html.HTMLEditorKit

/**
 * UI
 *
 * Created by Yii.Guxing on 2017/11/29
 */
object UI {

    // 使用`get() = ...`以保证获得实时`ScaledFont`
    val defaultFont: JBFont get() = JBFont.create(Font("Microsoft YaHei", Font.PLAIN, 14))

    val errorHTMLKit: HTMLEditorKit
        get() = UIUtil.getHTMLEditorKit().apply {
            with(styleSheet) {
                val font = Settings.instance
                        .takeIf { it.isOverrideFont }
                        ?.primaryFontFamily
                        ?.let { JBUI.Fonts.create(it, 15) }
                        ?: defaultFont.biggerOn(1f)

                addRule("body{color:#FF3333;font-family:${font.family};font-size:${font.size};text-align:center;}")
                addRule("a {color:#FF0000;}")
            }
        }

    fun getFont(fontFamily: String?, size: Int): JBFont = if (!fontFamily.isNullOrEmpty()) {
        JBUI.Fonts.create(fontFamily, size)
    } else {
        defaultFont.deriveFont(size.toFloat())
    }
}
package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Styles
import cn.yiiguxing.plugin.translate.model.QueryResult
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

/**
 * Styles
 *
 * Created by Yii.Guxing on 2017/11/29
 */
object Styles {
    val errorHTMLKit: HTMLEditorKit
        get() = UIUtil.getHTMLEditorKit().apply {
            with(styleSheet) {
                val font = JBUI.Fonts.label(15f)
                addRule("body{color:#FF3333;font-family:${font.family};font-size:${font.size};text-align:center;}")
                addRule("a {color:#FF0000;}")
            }
        }

    fun insertStylishResultText(textPane: JTextPane,
                                result: QueryResult,
                                explainsClickListener: Styles.OnTextClickListener?) {
    }
}
package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Styles
import cn.yiiguxing.plugin.translate.model.QueryResult
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import java.awt.Font
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

/**
 * Styles
 *
 * Created by Yii.Guxing on 2017/11/29
 */
object Styles {

    // 使用`get() = ...`以保证获得实时`ScaledFont`
    val defaultFont: JBFont get() = JBFont.create(Font("Microsoft YaHei", Font.PLAIN, 14))

    val errorHTMLKit: HTMLEditorKit
        get() = UIUtil.getHTMLEditorKit().apply {
            with(styleSheet) {
                val font = defaultFont.biggerOn(1f)
                addRule("body{color:#FF3333;font-family:${font.family};font-size:${font.size};text-align:center;}")
                addRule("a {color:#FF0000;}")
            }
        }

    fun insertStylishResultText(textPane: JTextPane,
                                result: QueryResult,
                                explainsClickListener: Styles.OnTextClickListener?) {
    }
}
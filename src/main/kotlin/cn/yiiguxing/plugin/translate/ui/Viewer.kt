package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.util.ui.JBFont
import java.awt.Font
import java.awt.Graphics
import javax.swing.JTextPane

/**
 * Viewer
 *
 * Created by Yii.Guxing on 2017/12/09
 */
class Viewer : JTextPane() {
    init {
        isOpaque = false
        isEditable = false

        editorKit = WarpEditorKit()
        font = JBFont.create(Font("Microsoft YaHei", Font.PLAIN, 14))
    }

    override fun paint(g: Graphics) {
        // 还原设置图像背景后的图形上下文，使图像背景失效。
        super.paint(IdeBackgroundUtil.getOriginalGraphics(g))
    }
}
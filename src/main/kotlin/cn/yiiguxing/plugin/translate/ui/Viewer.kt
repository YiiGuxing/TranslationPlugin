package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import java.awt.Graphics
import javax.swing.JTextPane


/**
 * Viewer
 */
open class Viewer : JTextPane() {

    init {
        isOpaque = false
        isEditable = false

        editorKit = WarpEditorKit()
        font = UI.defaultFont
    }

    fun disableSelection() {
        highlighter = null
    }

    override fun paint(g: Graphics) {
        // 还原设置图像背景后的图形上下文，使图像背景失效。
        super.paint(IdeBackgroundUtil.getOriginalGraphics(g))
    }
}
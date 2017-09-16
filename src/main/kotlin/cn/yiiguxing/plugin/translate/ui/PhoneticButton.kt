package cn.yiiguxing.plugin.translate.ui

import com.intellij.util.Consumer
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * Phonetic Button.
 */
class PhoneticButton(listener: Consumer<MouseEvent>) : IconButton(Icons.Speech, Icons.SpeechPressed, listener) {

    init {
        maximumSize = Dimension(Icons.Speech.iconWidth + MARGIN_LEFT + MARGIN_RIGHT, Icons.Speech.iconHeight)
        alignmentY = .84f
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, Icons.Speech.iconWidth + MARGIN_LEFT, height)
    }

    override fun setBounds(r: Rectangle) {
        super.setBounds(Rectangle(r.x, r.y, Icons.Speech.iconWidth + MARGIN_LEFT, r.height))
    }

    override fun paintIcon(g: Graphics, icon: Icon) {
        icon.paintIcon(this, g, MARGIN_LEFT, (height - icon.iconHeight) / 2)
    }

    companion object {
        private val MARGIN_LEFT = JBUI.scale(2)
        private val MARGIN_RIGHT = JBUI.scale(10)
    }
}
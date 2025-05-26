package cn.yiiguxing.plugin.translate.ui

import java.awt.Graphics
import javax.swing.text.DefaultCaret

/**
 * An empty caret that does not paint anything and keeps other caret functionality like selection.
 */
class EmptyCaret : DefaultCaret() {

    override fun isActive(): Boolean = false
    override fun isVisible(): Boolean = false
    override fun setVisible(e: Boolean) = Unit
    override fun getBlinkRate(): Int = 0
    override fun setBlinkRate(rate: Int) = Unit
    override fun paint(g: Graphics) = Unit

}
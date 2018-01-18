@file:Suppress("unused")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.icon.ProcessIcon
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * ProcessComponent
 *
 *
 * Created by Yii.Guxing on 2017/11/27
 */
class ProcessComponent(text: String, insets: Insets = JBUI.emptyInsets()) : JPanel(), Disposable {

    private val icon: ProcessIcon = ProcessIcon()
    private val label: JLabel = JLabel(text).apply {
        font = JBFont.create(font.deriveFont(18.0f))
        foreground = JBColor(0x4C4C4C, 0xCDCDCD)
    }

    val isRunning: Boolean = icon.isRunning

    var text: String
        get() = label.text
        set(value) {
            label.text = value
        }

    init {
        isOpaque = false
        layout = GridLayoutManager(1, 2, insets, JBUI.scale(5), 0)

        add(icon, GridConstraints().apply {
            column = 0
            hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
            vSizePolicy = GridConstraints.SIZEPOLICY_FIXED
            anchor = GridConstraints.ANCHOR_EAST
        })
        add(label, GridConstraints().apply {
            column = 1
            hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
            vSizePolicy = GridConstraints.SIZEPOLICY_FIXED
            anchor = GridConstraints.ANCHOR_WEST
        })
    }

    fun resume() = icon.resume()

    fun suspend() = icon.suspend()

    override fun dispose() {
        Disposer.dispose(icon)
    }

}

package cn.yiiguxing.plugin.translate.ui

import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension

/**
 * FixedSizeCardLayout
 *
 * Created by Yii.Guxing on 2017/11/8
 */
class FixedSizeCardLayout : CardLayout() {
    override fun preferredLayoutSize(parent: Container): Dimension = parent.getSize { preferredSize }

    override fun minimumLayoutSize(parent: Container): Dimension = parent.getSize { minimumSize }

    private inline fun Container.getSize(targetSize: Component.() -> Dimension): Dimension {
        synchronized(treeLock) {
            val insets = insets
            var w = 0
            var h = 0

            for (i in 0 until componentCount) {
                val comp = getComponent(i)
                if (comp.isVisible) {
                    val d = comp.targetSize()
                    w = d.width
                    h = d.height
                    break
                }
            }

            return Dimension(insets.left + insets.right + w + hgap * 2,
                    insets.top + insets.bottom + h + vgap * 2)
        }
    }
}
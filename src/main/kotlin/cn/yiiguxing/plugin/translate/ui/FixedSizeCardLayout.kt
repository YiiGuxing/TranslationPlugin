package cn.yiiguxing.plugin.translate.ui

import java.awt.CardLayout
import java.awt.Container
import java.awt.Dimension

/**
 * FixedSizeCardLayout
 *
 * Created by Yii.Guxing on 2017/11/8
 */
class FixedSizeCardLayout : CardLayout() {
    override fun preferredLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            var w = 0
            var h = 0

            for (i in 0 until parent.componentCount) {
                val comp = parent.getComponent(i)
                if (comp.isVisible) {
                    val d = comp.preferredSize
                    w = d.width
                    h = d.height
                    break
                }
            }
            return Dimension(insets.left + insets.right + w + hgap * 2,
                    insets.top + insets.bottom + h + vgap * 2)
        }
    }

    override fun minimumLayoutSize(parent: Container): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            var w = 0
            var h = 0

            for (i in 0 until parent.componentCount) {
                val comp = parent.getComponent(i)
                if (comp.isVisible) {
                    val d = comp.minimumSize
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
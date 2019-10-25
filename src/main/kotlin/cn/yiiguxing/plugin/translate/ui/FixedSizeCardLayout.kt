package cn.yiiguxing.plugin.translate.ui

import java.awt.CardLayout
import java.awt.Component
import java.awt.Container
import java.awt.Dimension

/**
 * FixedSizeCardLayout
 */
class FixedSizeCardLayout : CardLayout() {

    override fun preferredLayoutSize(parent: Container): Dimension = parent.getSize { preferredSize }
    override fun minimumLayoutSize(parent: Container): Dimension = parent.getSize { minimumSize }
    override fun maximumLayoutSize(parent: Container): Dimension = parent.getSize { maximumSize }

    private inline fun Container.getSize(targetSize: Component.() -> Dimension): Dimension {
        synchronized(treeLock) {
            var w = 0
            var h = 0

            for (i in 0 until componentCount) {
                val comp = getComponent(i)
                if (comp.isVisible) {
                    comp.targetSize().let { (tw, th) ->
                        w = tw
                        h = th
                    }
                    break
                }
            }

            return with(insets) {
                Dimension(left + right + w + hgap * 2, top + bottom + h + vgap * 2)
            }
        }
    }

}
package cn.yiiguxing.plugin.translate.ui

import com.intellij.util.ui.AbstractLayoutManager
import java.awt.Component
import java.awt.Container
import java.awt.Dimension

/**
 * FrameLayout
 */
class FrameLayout : AbstractLayoutManager() {

    override fun preferredLayoutSize(parent: Container): Dimension = parent.getSize { preferredSize }
    override fun minimumLayoutSize(parent: Container): Dimension = parent.getSize { minimumSize }
    override fun maximumLayoutSize(parent: Container): Dimension = parent.getSize { maximumSize }

    private inline fun Container.getSize(targetSize: Component.() -> Dimension): Dimension {
        synchronized(treeLock) {
            var w = 0
            var h = 0
            for (i in 0 until componentCount) {
                getComponent(i)
                        .takeIf { it.isVisible }
                        ?.targetSize()
                        ?.let { (tw, th) ->
                            w = maxOf(tw, w)
                            h = maxOf(th, h)
                        }
            }

            return with(insets) {
                Dimension(left + right + w, top + bottom + h)
            }
        }
    }

    override fun layoutContainer(parent: Container) = with(parent) {
        synchronized(treeLock) {
            val insets = insets
            val pw = width - insets.left - insets.right
            val ph = height - insets.top - insets.bottom

            for (i in 0 until componentCount) {
                getComponent(i)
                        .takeIf { it.isVisible }
                        ?.apply {
                            val (w, h) = preferredSize
                            val x = alignmentX.let { it * (pw - w) }.toInt()
                            val y = alignmentY.let { it * (ph - h) }.toInt()
                            setBounds(x, y, w, h)
                        }
            }
        }
    }
}
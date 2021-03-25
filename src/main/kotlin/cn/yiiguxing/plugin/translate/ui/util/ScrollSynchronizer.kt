package cn.yiiguxing.plugin.translate.ui.util

import javax.swing.BoundedRangeModel
import javax.swing.JScrollBar
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A scroll synchronizer that allows multiple scroll bars to scroll in sync.
 */
class ScrollSynchronizer private constructor(private vararg val scrollBars: JScrollBar) : ChangeListener {

    var isEnabled: Boolean = true

    private var isBroadcast: Boolean = false

    init {
        for (scrollBar in scrollBars) {
            scrollBar.model.addChangeListener(this)
        }
    }

    /**
     * Sets [scroll value][value] (0-1f) for all scrollbars.
     */
    @Suppress("unused")
    fun setScroll(value: Float) {
        isBroadcast = true
        val fixedValue = max(0f, min(value, 1f))
        for (scrollBar in scrollBars) {
            scrollBar.percentValue = fixedValue
        }
        isBroadcast = false
    }

    override fun stateChanged(e: ChangeEvent) {
        if (isEnabled && !isBroadcast) {
            isBroadcast = true
            for (scrollBar in scrollBars) {
                scrollBar.syncScrollWith(e.source as BoundedRangeModel)
            }
            isBroadcast = false
        }
    }

    private fun JScrollBar.syncScrollWith(target: BoundedRangeModel) {
        if (model !== target) {
            percentValue = target.percentValue
        }
    }

    /**
     * Release the synchronizer
     */
    fun release() {
        for (scrollBar in scrollBars) {
            scrollBar.model.removeChangeListener(this)
        }
    }

    companion object {
        /**
         * Returns a scroll synchronizer with multiple scroll bars.
         */
        fun syncScroll(scrollBar1: JScrollBar, scrollBar2: JScrollBar, vararg others: JScrollBar): ScrollSynchronizer {
            return ScrollSynchronizer(scrollBar1, scrollBar2, *others)
        }

        private val BoundedRangeModel.percentValue: Float?
            get() {
                val distance = maximum - extent - minimum
                return if (distance != 0) {
                    (value - minimum).toFloat() / distance.toFloat()
                } else null
            }

        private var JScrollBar.percentValue: Float?
            get() = model.percentValue
            set(percentValue) {
                if (percentValue != null) {
                    value = minimum + (percentValue * (maximum - visibleAmount - minimum)).roundToInt()
                }
            }
    }
}


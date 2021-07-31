package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.balloon.BalloonImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.PositionTracker
import java.awt.Point

/**
 * BalloonPositionTracker
 */
class BalloonPositionTracker(
    private val editor: Editor,
    private val caretRangeMarker: RangeMarker
) : PositionTracker<Balloon>(editor.contentComponent) {

    private var lastLocation: RelativePoint? = null

    init {
        Disposer.register(this) {
            lastLocation = null
            caretRangeMarker.dispose()
        }
    }

    override fun recalculateLocation(balloon: Balloon): RelativePoint {
        val last = lastLocation
        val location = editor.getBalloonLocation(caretRangeMarker)
        (balloon as? BalloonImpl)?.setLostPointer(location == null)
        if (last != null && location == null) {
            return last
        }

        return editor.guessBestBalloonLocation(location).also {
            lastLocation = it
        }
    }
}

private fun Editor.getBalloonLocation(caretRangeMarker: RangeMarker): Point? {
    if (isDisposed || !caretRangeMarker.isValid) {
        return null
    }

    val startPosition = offsetToVisualPosition(caretRangeMarker.startOffset, true, false)
    val endPosition = offsetToVisualPosition(caretRangeMarker.endOffset, false, false)
    val startPoint = visualPositionToXY(startPosition)
    val endPoint = visualPositionToXY(endPosition)

    val lineHeight = lineHeight
    val centerX = ((startPoint.x + endPoint.x) * 0.5f).toInt()
    val x = minOf(centerX, endPoint.x)
    val y = endPoint.y + lineHeight

    return scrollingModel.visibleArea.let {
        if (it.contains(x, y)) Point(x, y) else null
    }
}

private fun Editor.guessBestBalloonLocation(point: Point?): RelativePoint {
    val location = point ?: with(scrollingModel.visibleArea) {
        Point(x + width / 3, y + height / 2)
    }

    return RelativePoint(contentComponent, location)
}
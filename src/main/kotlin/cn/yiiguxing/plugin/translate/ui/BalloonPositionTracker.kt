package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.BalloonImpl
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
        val location = editor.getBalloonLocation(balloon as? BalloonImpl, caretRangeMarker)
        if (last != null && location == null) {
            return last
        }

        return editor.guessBestBalloonLocation(location).also {
            lastLocation = it
        }
    }
}

private fun Editor.getBalloonLocation(
    balloon: BalloonImpl?,
    caretRangeMarker: RangeMarker
): Point? {
    if (isDisposed || !caretRangeMarker.isValid) {
        return null
    }

    val startPosition = offsetToVisualPosition(caretRangeMarker.startOffset, true, false)
    val endPosition = offsetToVisualPosition(caretRangeMarker.endOffset, false, false)
    val startPoint = visualPositionToXY(startPosition)
    val endPoint = visualPositionToXY(endPosition)

    val centerX = ((startPoint.x + endPoint.x) * 0.5f).toInt()
    val x = minOf(centerX, endPoint.x)
    val y = if (balloon?.isShowingAbove() == true) {
        startPoint.y
    } else {
        endPoint.y + lineHeight
    }

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


/**
 * [BalloonImpl] 的 `ABOVE`/`BELOW` 静态字段虽然声明为 `public`，
 * 但其类型 `AbstractPosition` 是 `private` 内部类，直接访问该字段会在运行时抛
 * `IllegalAccessError`。这里通过公开方法 [BalloonImpl.getAbstractPositionFor]
 * 获取对应的位置实例（方法调用不受返回类型可见性限制），
 * 再与 [BalloonImpl.getPosition] 的返回值做引用比较。
 */
private val balloonAbovePosition: Any? = try {
    @Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
    BalloonImpl.getAbstractPositionFor(Balloon.Position.above)
} catch (_: Throwable) {
    null
}

@Suppress("INFERRED_INVISIBLE_RETURN_TYPE_WARNING")
private fun BalloonImpl.actualPosition(): Any? = position

internal fun BalloonImpl.isShowingAbove(): Boolean {
    val current = actualPosition() ?: return false
    // 优先引用比较，`toString()` 兜底（`AbstractPosition` 的 `toString()` 返回简单类名）。
    return current === balloonAbovePosition || current.toString() == "Above"
}

package cn.yiiguxing.plugin.translate.ui.compat

import cn.yiiguxing.plugin.translate.util.IdeVersion
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import java.awt.Point
import java.lang.reflect.Method
import javax.swing.JComponent

enum class GotItTooltipPosition {
    TOP, RIGHT, BOTTOM, LEFT
}

private val TOP: (JComponent) -> Point = { Point(it.width / 2, 0) }
private val LEFT: (JComponent) -> Point = { Point(0, it.height / 2) }
private val RIGHT: (JComponent) -> Point = { Point(it.width, it.height / 2) }
private val BOTTOM: (JComponent) -> Point = { Point(it.width / 2, it.height) }

private val TOP_COMPAT_212: (JComponent, Any) -> Point = { it, _ -> Point(it.width / 2, 0) }
private val RIGHT_COMPAT_212: (JComponent, Any) -> Point = { it, _ -> Point(it.width, it.height / 2) }
private val BOTTOM_COMPAT_212: (JComponent, Any) -> Point = { it, _ -> Point(it.width / 2, it.height) }
private val LEFT_COMPAT_212: (JComponent, Any) -> Point = { it, _ -> Point(0, it.height / 2) }

private val LOG: Logger = Logger.getInstance("cn.yiiguxing.plugin.translate.ui.compat.GotItTooltipCompatKt")

private val SHOW_METHOD: Method? by lazy {
    try {
        GotItTooltip::class.java.getDeclaredMethod(
            "show",
            JComponent::class.java,
            TOP.javaClass.interfaces[0]
        )
    } catch (e: Throwable) {
        LOG.w("Failed to get GotItTooltip.show method", e)
        null
    }
}
private val SHOW_METHOD_COMPAT_212: Method? by lazy {
    try {
        GotItTooltip::class.java.getDeclaredMethod(
            "show",
            JComponent::class.java,
            TOP_COMPAT_212.javaClass.interfaces[0]
        )
    } catch (e: Throwable) {
        LOG.w("Failed to get GotItTooltip.show method", e)
        null
    }
}


fun GotItTooltip.show(component: JComponent, position: GotItTooltipPosition) {
    val isIde212 = IdeVersion >= IdeVersion.IDE2021_2
    val positionProvider = when (position) {
        GotItTooltipPosition.TOP -> if (isIde212) TOP_COMPAT_212 else TOP
        GotItTooltipPosition.LEFT -> if (isIde212) LEFT_COMPAT_212 else LEFT
        GotItTooltipPosition.RIGHT -> if (isIde212) RIGHT_COMPAT_212 else RIGHT
        GotItTooltipPosition.BOTTOM -> if (isIde212) BOTTOM_COMPAT_212 else BOTTOM
    }
    val showMethod = if (isIde212) SHOW_METHOD_COMPAT_212 else SHOW_METHOD
    if (showMethod == null) {
        LOG.w("Failed to show GotItTooltip")
        release()
        return
    }

    try {
        showMethod.invoke(this, component, positionProvider)
    } catch (e: Throwable) {
        LOG.w("Failed to invoke GotItTooltip.show method", e)
        release()
    }
}

private fun GotItTooltip.release() {
    PropertiesComponent.getInstance().setValue("${GotItTooltip.PROPERTY_PREFIX}.$id", "1")
    Disposer.dispose(this)
}
package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import java.awt.Color
import javax.swing.Icon

/**
 * ActionLink
 */
class ActionLink(
        text: String? = null,
        icon: Icon? = null,
        hoveringIcon: Icon? = null,
        private val action: (ActionLink) -> Unit
) : LinkLabel<Any?>(text, icon), LinkListener<Any?> {

    var visitedColor: Color? = null
    var activeColor: Color? = null
    var normalColor: Color? = null

    init {
        setHoveringIcon(hoveringIcon)
        setListener(this, null)
    }

    override fun getVisited(): Color {
        return visitedColor ?: super.getVisited()
    }

    override fun getActive(): Color {
        return activeColor ?: super.getActive()
    }

    override fun getNormal(): Color {
        return normalColor ?: super.getNormal()
    }

    override fun linkSelected(aSource: LinkLabel<Any?>, aLinkData: Any?) {
        if (isEnabled) {
            action(this)
        }
    }
}
package cn.yiiguxing.plugin.translate.ui

import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import java.awt.Color

/**
 * ActionLink
 *
 * Created by Yii.Guxing on 2017/12/15
 */
class ActionLink(
        text: String? = null,
        private val action: (ActionLink) -> Unit
) : LinkLabel<Any?>(text, null), LinkListener<Any?> {

    var visitedColor: Color? = null
    var activeColor: Color? = null
    var normalColor: Color? = null

    init {
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

    override fun linkSelected(aSource: LinkLabel<*>, aLinkData: Any?) {
        action(this)
    }
}
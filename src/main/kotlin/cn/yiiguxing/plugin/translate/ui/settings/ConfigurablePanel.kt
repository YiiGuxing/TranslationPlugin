package cn.yiiguxing.plugin.translate.ui.settings

import javax.swing.JComponent

/**
 * ConfigurablePanel
 *
 */
interface ConfigurablePanel {

    val component: JComponent

    val isModified: Boolean

    fun reset()

    fun apply()

}

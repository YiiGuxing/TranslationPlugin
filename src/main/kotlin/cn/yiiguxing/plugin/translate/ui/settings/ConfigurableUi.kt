package cn.yiiguxing.plugin.translate.ui.settings

import com.intellij.openapi.Disposable
import javax.swing.JComponent

/**
 * ConfigurablePanel
 *
 */
interface ConfigurableUi : Disposable {

    val component: JComponent

    val preferredFocusedComponent: JComponent

    val isModified: Boolean

    fun reset()

    fun apply()

    override fun dispose() {}

}

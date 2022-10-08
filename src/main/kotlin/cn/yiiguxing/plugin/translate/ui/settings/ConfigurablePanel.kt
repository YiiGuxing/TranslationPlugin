package cn.yiiguxing.plugin.translate.ui.settings

import com.intellij.openapi.Disposable
import javax.swing.JComponent

/**
 * ConfigurablePanel
 *
 */
interface ConfigurablePanel : Disposable {

    val component: JComponent

    val isModified: Boolean

    fun reset()

    fun apply()

    override fun dispose() {}

}

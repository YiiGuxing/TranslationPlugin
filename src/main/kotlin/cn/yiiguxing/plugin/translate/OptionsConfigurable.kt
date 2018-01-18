package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.ui.settings.ConfigurablePanel
import cn.yiiguxing.plugin.translate.ui.settings.SettingsPanel
import cn.yiiguxing.plugin.translate.util.AppStorage
import cn.yiiguxing.plugin.translate.util.Settings
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

/**
 * 选项配置
 */
class OptionsConfigurable : SearchableConfigurable, Disposable {

    private var configurablePanel: ConfigurablePanel? = null

    override fun getId(): String = "yiiguxing.plugin.translate"

    override fun enableSearch(option: String?): Runnable? = null

    override fun getDisplayName(): String = "Translation"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent = SettingsPanel(Settings, AppStorage).let {
        configurablePanel = it
        it.component
    }

    override fun isModified(): Boolean = configurablePanel?.isModified ?: false

    override fun apply() {
        configurablePanel?.apply()
    }

    override fun reset() {
        configurablePanel?.reset()
    }

    override fun disposeUIResources() {
        Disposer.dispose(this)
    }

    override fun dispose() {
        configurablePanel = null
    }

    companion object {
        fun showSettingsDialog(project: Project?) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, OptionsConfigurable::class.java)
        }
    }
}

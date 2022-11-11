package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.HelpTopic
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.adaptedMessage
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslationStates
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

/**
 * 选项配置
 */
class TranslationConfigurable : SearchableConfigurable {

    private val ui: ClearableLazyValue<ConfigurableUi> = ClearableLazyValue.create {
        SettingsPanel(Settings, TranslationStates)
    }

    override fun getId(): String = TranslationPlugin.PLUGIN_ID

    override fun getDisplayName(): String = adaptedMessage("settings.page.name")

    override fun getHelpTopic(): String = HelpTopic.DEFAULT.id

    override fun createComponent(): JComponent = ui.value.component

    override fun getPreferredFocusedComponent(): JComponent = ui.value.preferredFocusedComponent

    override fun isModified(): Boolean = ui.value.isModified

    override fun apply() {
        ui.value.apply()
    }

    override fun reset() {
        ui.value.reset()
    }

    override fun disposeUIResources() {
        ui.value.let { Disposer.dispose(it) }
        ui.drop()
    }

    companion object {
        fun showSettingsDialog(project: Project? = null) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(project?.takeUnless { it.isDisposed }, TranslationConfigurable::class.java)
        }
    }
}

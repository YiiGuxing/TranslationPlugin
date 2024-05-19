package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.*
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

internal interface TranslationConfigurable : SearchableConfigurable {
    companion object {
        /**
         * Show the settings dialog.
         */
        fun showSettingsDialog(project: Project? = null) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(project?.takeUnless { it.isDisposed }, TranslationConfigurable::class.java)
        }
    }
}

/**
 * 选项配置
 */
internal class TranslationConfigurableImpl : TranslationConfigurable {

    private val ui: ClearableLazyValue<ConfigurableUi> = ClearableLazyValue.create {
        SettingsPanel(Settings.getInstance(), TranslationStates.getInstance())
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
}

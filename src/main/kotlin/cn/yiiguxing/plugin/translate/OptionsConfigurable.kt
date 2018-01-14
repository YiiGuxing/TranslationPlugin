package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.ui.ConfigurablePanel
import cn.yiiguxing.plugin.translate.ui.SettingsPanel
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

    private val mSettings: Settings = Settings.instance
    private val mAppStorage: AppStorage = AppStorage.instance

    private var mPanel: ConfigurablePanel? = null

    override fun getId(): String = "yiiguxing.plugin.translate"

    override fun enableSearch(option: String?): Runnable? = null

    override fun getDisplayName(): String = "Translation"

    override fun getHelpTopic(): String? = null

    override fun createComponent(): JComponent = with(SettingsPanel(mSettings, mAppStorage)) {
        mPanel = this@with
        component
    }

    override fun isModified(): Boolean = mPanel?.isModified ?: false

    override fun apply() {
        mPanel?.apply()
    }

    override fun reset() {
        mPanel?.reset()
    }

    override fun disposeUIResources() {
        Disposer.dispose(this)
    }

    override fun dispose() {
        mPanel = null
    }

    companion object {
        fun showSettingsDialog(project: Project?) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, OptionsConfigurable::class.java)
        }
    }
}

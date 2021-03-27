package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.action.TranslatorAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.service.TranslationUIManager
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.Application
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

class TranslatorWidgetFactory : StatusBarWidgetFactory {
    init {
        subscribeToSettingsChange()
    }

    override fun getId(): String {
        return TranslatorWidget.ID
    }

    override fun getDisplayName(): String {
        return message("choose.translator.widget.name")
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun isAvailable(project: Project): Boolean {
        return TranslatorAction.availableActions().size > 1
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return TranslatorWidget(project)
    }

    override fun canBeEnabledOn(project: StatusBar): Boolean {
        return true
    }

    private fun subscribeToSettingsChange() {
        Application.messageBus
            .connect(TranslationUIManager.disposable())
            .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                override fun onTranslatorConfigurationChanged() {
                    updateWidgetsInAllProjects()
                }

                override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
                    updateWidgetsInAllProjects()
                }
            })

    }

    private fun updateWidgetsInAllProjects() {
        // To fix the Settings service cycle initialization problem
        Application.invokeLater {
            Application.runReadAction {
                val projects = ProjectManager.getInstance().openProjects
                projects.forEach {
                    it.getService(StatusBarWidgetsManager::class.java)
                        .updateWidget(this@TranslatorWidgetFactory)
                }
            }
        }
    }
}
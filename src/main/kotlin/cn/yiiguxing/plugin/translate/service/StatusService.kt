package cn.yiiguxing.plugin.translate.service

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.WindowOption
import cn.yiiguxing.plugin.translate.ui.TranslatorWidget
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.d
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

class StatusService(private val project: Project) : Disposable {

    private val translatorWidget = TranslatorWidget(project)

    init {
        project.messageBus.connect(this).subscribeToSettingsChangeEvents()
    }

    /**
     * 显示状态栏图标
     */
    fun installStatusWidget() {
        if (Settings.showStatusIcon) {
            translatorWidget.install()
        }
    }

    private fun updateStatus(settings: Settings, option: WindowOption) {
        if (option != WindowOption.STATUS_ICON) {
            return
        }

        if (settings.showStatusIcon) {
            translatorWidget.install()
        } else {
            translatorWidget.uninstall()
        }
    }

    private fun MessageBusConnection.subscribeToSettingsChangeEvents() {
        subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
            override fun onWindowOptionsChanged(settings: Settings, option: WindowOption) {
                updateStatus(settings, option)
            }

            override fun onTranslatorChanged(settings: Settings, translatorId: String) {
                translatorWidget.update()
            }
        })
    }

    override fun dispose() {
        translatorWidget.uninstall()
        LOGGER.d("Status service disposed.")
    }

    companion object {
        private val LOGGER: Logger = Logger.getInstance(StatusService::class.java)

        fun getInstance(project: Project): StatusService = ServiceManager.getService(project, StatusService::class.java)
    }

}
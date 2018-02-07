package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.WindowOption
import cn.yiiguxing.plugin.translate.action.TranslatorActionGroup
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.invokeOnDispatchThread
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.MemoryUsagePanel
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.messages.MessageBusConnection
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * TranslatorWidget
 *
 * Created by Yii.Guxing on 2018/1/19
 */
class TranslatorWidget(
        private val project: Project
) : StatusBarWidget, StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe {

    private val translateService = TranslateService
    private var statusBar: StatusBar? = null
    private var isInstalled: Boolean = false

    init {
        project.messageBus.connect(project).subscribeToSettingsChangeEvents()
        Disposer.register(project, Disposable { uninstall() })
    }

    @Suppress("FunctionName")
    override fun ID(): String = javaClass.name

    override fun getTooltipText(): String = translateService.translator.name

    override fun getIcon(): Icon = translateService.translator.icon

    override fun copy(): StatusBarWidget = TranslatorWidget(project)

    override fun getPresentation(type: StatusBarWidget.PlatformType): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        getPopupStep(it.component).apply {
            val at = Point(0, -content.preferredSize.height)
            show(RelativePoint(it.component, at))
        }
    }

    private fun getPopupStep(component: Component): ListPopup {
        val group = TranslatorActionGroup()
        val context = DataManager.getInstance().getDataContext(component)
        return JBPopupFactory.getInstance()
                .createActionGroupPopup(TranslatorActionGroup.TITLE, group, context, false, null, 5)
    }

    override fun dispose() {
        statusBar = null
        isInstalled = false
    }

    private fun update() {
        invokeOnDispatchThread {
            statusBar?.updateWidget(ID())
        }
    }

    fun install() {
        invokeOnDispatchThread {
            if (!isInstalled) {
                isInstalled = true
                WindowManager.getInstance()
                        .getStatusBar(project)
                        ?.addWidget(this, "before ${MemoryUsagePanel.WIDGET_ID}", project)
            }
        }
    }

    fun uninstall() {
        invokeOnDispatchThread {
            statusBar?.removeWidget(ID())
        }
    }

    private fun MessageBusConnection.subscribeToSettingsChangeEvents() {
        subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
            override fun onWindowOptionsChanged(settings: Settings, option: WindowOption) {
                if (option != WindowOption.STATUS_ICON) {
                    return
                }

                if (settings.showStatusIcon) {
                    install()
                } else {
                    uninstall()
                }
            }

            override fun onTranslatorChanged(settings: Settings, translatorId: String) {
                update()
            }
        })
    }
}
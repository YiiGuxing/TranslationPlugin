package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.action.SwitchTranslationEngineAction
import cn.yiiguxing.plugin.translate.compat.ui.GotItTooltipPosition
import cn.yiiguxing.plugin.translate.compat.ui.show
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.ide.DataManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetWrapper
import com.intellij.ui.GotItTooltip
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Consumer
import com.intellij.util.messages.MessageBusConnection
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

/**
 * TranslationWidget
 */
class TranslationWidget(private val project: Project) : StatusBarWidget.IconPresentation, CustomStatusBarWidget {

    private val widgetComponent: JComponent by lazy { StatusBarWidgetWrapper.wrap(this) }

    override fun ID(): String = ID

    override fun getTooltipText(): String = TranslateService.translator.name

    override fun getIcon(): Icon = TranslateService.translator.icon

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getComponent(): JComponent = widgetComponent

    override fun install(statusBar: StatusBar) {
        if (project.isDisposed) {
            return
        }

        project.messageBus.connect(this).subscribeToSettingsChangeEvents(statusBar)
        DumbService.getInstance(project).smartInvokeLater {
            if (!project.isDisposed) {
                showGotItTooltipIfNeed()
            }
        }
    }

    private fun showGotItTooltipIfNeed() {
        val id = "${TranslationPlugin.PLUGIN_ID}.tooltip.new.translation.engines.openai"
        val message = message("got.it.tooltip.text.new.translation.engines")
        GotItTooltip(id, message, this)
            .withHeader(message("got.it.tooltip.title.new.translation.engines"))
            .show(widgetComponent, GotItTooltipPosition.TOP)
    }

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        getPopupStep(it.component).apply {
            val at = Point(0, -content.preferredSize.height)
            show(RelativePoint(it.component, at))
        }
    }

    private fun getPopupStep(component: Component): ListPopup {
        val context = DataManager.getInstance().getDataContext(component)
        return SwitchTranslationEngineAction.createTranslationEnginesPopup(context)
    }

    override fun dispose() {}

    companion object {
        const val ID = "Translation.Widget"

        private fun MessageBusConnection.subscribeToSettingsChangeEvents(statusBar: StatusBar) {
            subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
                    statusBar.updateWidget(ID)
                }
            })
        }
    }
}
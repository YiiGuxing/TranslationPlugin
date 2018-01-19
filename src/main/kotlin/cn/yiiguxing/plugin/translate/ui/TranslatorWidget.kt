package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.invokeOnDispatchThread
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.status.MemoryUsagePanel
import com.intellij.util.Consumer
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
    private val messageBus = project.messageBus.connect(this)
    private lateinit var statusBar: StatusBar
    private var isDisposed: Boolean = false

    init {
        Disposer.register(project, this)
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

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        // TODO 添加点击切换翻译引擎支持
        return null
    }

    override fun dispose() {
        isDisposed = true
        messageBus.disconnect()
    }

    private fun update() {
        statusBar.updateWidget(ID())
    }

    fun install() {
        if (isDisposed) {
            return
        }

        WindowManager.getInstance()
                .getStatusBar(project)
                ?.addWidget(this, "before ${MemoryUsagePanel.WIDGET_ID}", project)
        messageBus.subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
            override fun onTranslatorChanged(settings: Settings, translatorId: String) {
                invokeOnDispatchThread(::update)
            }
        })
    }
}
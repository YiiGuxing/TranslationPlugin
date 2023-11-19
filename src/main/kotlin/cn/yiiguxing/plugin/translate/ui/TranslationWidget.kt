package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.action.TranslationEngineActionGroup
import cn.yiiguxing.plugin.translate.compat.ui.GotItTooltipPosition
import cn.yiiguxing.plugin.translate.compat.ui.show
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.invokeLaterIfNeeded
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.impl.status.TextPanel.WithIconAndArrows
import com.intellij.ui.ClickListener
import com.intellij.ui.GotItTooltip
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.concurrency.runAsync
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.JComponent

/**
 * TranslationWidget
 */
class TranslationWidget(private val project: Project) : WithIconAndArrows(), IconLikeCustomStatusBarWidget {

    companion object {
        const val ID = "Translation.Widget"
    }

    private var isLoadingTranslationEngines = false

    private var isDisposed = false

    init {
        setTextAlignment(CENTER_ALIGNMENT)
    }

    override fun ID(): String = ID

    override fun getComponent(): JComponent = this

    override fun install(statusBar: StatusBar) {
        if (project.isDisposed) {
            return
        }

        setupClickListener()
        subscribeToSettingsChangeEvents(statusBar)
        update { statusBar.updateWidget(ID) }
        scheduleGotItTooltip()
    }

    private fun setupClickListener() {
        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                if (!project.isDisposed) {
                    showPopup()
                }
                return true
            }
        }.installOn(this, true)
    }

    private fun subscribeToSettingsChangeEvents(statusBar: StatusBar) {
        project.messageBus
            .connect(this)
            .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
                    update { statusBar.updateWidget(ID) }
                }
            })
    }

    private fun update(onUpdated: (() -> Unit)? = null) {
        if (project.isDisposed) return
        invokeLaterIfNeeded {
            if (project.isDisposed) return@invokeLaterIfNeeded

            toolTipText = TranslateService.translator.name
            icon = TranslateService.translator.icon
            onUpdated?.invoke()
            repaint()
        }
    }

    private fun scheduleGotItTooltip() {
        DumbService.getInstance(project).smartInvokeLater {
            if (!project.isDisposed) {
                showGotItTooltipIfNeed()
            }
        }
    }

    private fun showGotItTooltipIfNeed() {
        if (isDisposed) {
            return
        }

        val id = "${TranslationPlugin.PLUGIN_ID}.tooltip.new.translation.engines.openai"
        val message = message("got.it.tooltip.text.new.translation.engines")
        GotItTooltip(id, message, this)
            .withHeader(message("got.it.tooltip.title.new.translation.engines"))
            .show(this, GotItTooltipPosition.TOP)
    }

    private fun showPopup() {
        if (isLoadingTranslationEngines) {
            return
        }

        isLoadingTranslationEngines = true
        val widgetRef = DisposableRef.create(this, this)
        runAsync { TranslationEngineActionGroup() }
            .expireWith(this)
            .successOnUiThread(widgetRef) { widget, group ->
                val context = DataManager.getInstance().getDataContext(widget)
                val popup = group.createActionPopup(context)
                val at = Point(0, -popup.content.preferredSize.height)
                popup.show(RelativePoint(widget, at))
            }
            .onError {
                logger<TranslationWidget>().warn("Failed to show translation engines popup.", it)
            }
            .finishOnUiThread(widgetRef, ModalityState.any()) { widget, _ ->
                widget.isLoadingTranslationEngines = false
            }
            .disposeAfterProcessing(widgetRef)
    }

    override fun dispose() {
        isDisposed = true
    }

}
package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.action.TranslationEngineActionGroup
import cn.yiiguxing.plugin.translate.compat.ui.GotItTooltipPosition
import cn.yiiguxing.plugin.translate.compat.ui.show
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.update.UpdateListener
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.*
import cn.yiiguxing.plugin.translate.util.invokeLaterIfNeeded
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.impl.status.TextPanel.WithIconAndArrows
import com.intellij.ui.ClickListener
import com.intellij.ui.GotItTooltip
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.runAsync
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
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

            TranslateService.getInstance().translator.let { translator ->
                toolTipText = translator.name
                icon = translator.icon
            }
            onUpdated?.invoke()
            repaint()
        }
    }

    private fun scheduleGotItTooltip() {
        val disposable = Disposer.newDisposable(this, "GotItTooltip Scheduler Disposable")
        project.messageBus
            .connect(disposable)
            .subscribe(UpdateListener.TOPIC, object : UpdateListener {
                override fun onPostUpdate(hasUpdate: Boolean) {
                    Disposer.dispose(disposable)
                    val runnable = {
                        if (!project.isDisposed) {
                            DumbService.getInstance(project).smartInvokeLater {
                                showGotItTooltipIfNeed()
                            }
                        }
                    }
                    if (hasUpdate) {
                        AppExecutorUtil.getAppScheduledExecutorService().schedule(runnable, 1, TimeUnit.SECONDS)
                    } else {
                        runnable()
                    }
                }
            })
    }

    private fun showGotItTooltipIfNeed() {
        if (!isShowing || isDisposed || project.isDisposed) {
            return
        }

        val id = TranslationPlugin.generateId("tooltip.new.engines.tts")
        val message = message("got.it.tooltip.text.new.engines")
        GotItTooltip(id, message, this)
            .withHeader(message("got.it.tooltip.title.new.engines"))
            .show(this, GotItTooltipPosition.TOP)
    }

    private fun showPopup() {
        if (isLoadingTranslationEngines) {
            return
        }

        isLoadingTranslationEngines = true
        val widgetRef = DisposableRef.create(this, this)

        asyncLatch { latch ->
            runAsync {
                latch.await()
                TranslationEngineActionGroup()
            }
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
    }

    override fun dispose() {
        isDisposed = true
    }

}
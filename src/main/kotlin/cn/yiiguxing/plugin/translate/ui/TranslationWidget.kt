package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.SettingsChangeListener
import cn.yiiguxing.plugin.translate.TranslationPlugin
import cn.yiiguxing.plugin.translate.action.TranslationEngineActionGroup
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import cn.yiiguxing.plugin.translate.update.UpdateListener
import cn.yiiguxing.plugin.translate.util.DisposableRef
import cn.yiiguxing.plugin.translate.util.concurrent.asyncLatch
import cn.yiiguxing.plugin.translate.util.concurrent.disposeAfterProcessing
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import cn.yiiguxing.plugin.translate.util.invokeLaterIfNeeded
import com.intellij.ide.DataManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.ui.ClickListener
import com.intellij.ui.GotItTooltip
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.runAsync
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * TranslationWidget
 */
class TranslationWidget(private val project: Project) : IconLikeCustomStatusBarWidget {

    companion object {
        const val ID = "Translation.Widget"
    }

    private var isDisposed = false

    private val component by lazy { WidgetComponent(this) }

    override fun ID(): String = ID

    override fun getComponent(): JComponent = component

    override fun dispose() {
        isDisposed = true
    }

    private class WidgetComponent(private val widget: TranslationWidget) : JPanel(BorderLayout()) {
        private val icon = JLabel().apply {
            isOpaque = false
            verticalAlignment = SwingConstants.CENTER
            horizontalAlignment = SwingConstants.CENTER
        }

        private var isLoadingTranslationEngines = false

        init {
            isOpaque = false
            add(icon, BorderLayout.CENTER)
            updateStatus()
        }

        override fun addNotify() {
            super.addNotify()
            install()
        }

        private fun install() {
            if (widget.project.isDisposed) {
                return
            }

            setupClickListener()
            subscribeToSettingsChangeEvents()
            update()
            scheduleGotItTooltip()
        }

        private fun setupClickListener() {
            object : ClickListener() {
                override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                    if (!widget.project.isDisposed) {
                        showPopup()
                    }
                    return true
                }
            }.installOn(this, true)
        }

        private fun subscribeToSettingsChangeEvents() {
            widget.project.messageBus
                .connect(widget)
                .subscribe(SettingsChangeListener.TOPIC, object : SettingsChangeListener {
                    override fun onTranslatorChanged(settings: Settings, translationEngine: TranslationEngine) {
                        update()
                    }
                })
        }

        private fun update() {
            if (widget.project.isDisposed) return
            invokeLaterIfNeeded {
                if (!widget.project.isDisposed) {
                    updateStatus()
                }
            }
        }

        private fun updateStatus() {
            val translator = TranslateService.getInstance().translator
            toolTipText = translator.name
            icon.icon = translator.icon
        }

        private fun scheduleGotItTooltip() {
            val disposable = Disposer.newDisposable(widget, "GotItTooltip Scheduler Disposable")
            widget.project.messageBus
                .connect(disposable)
                .subscribe(UpdateListener.TOPIC, object : UpdateListener {
                    override fun onPostUpdate(hasUpdate: Boolean) {
                        Disposer.dispose(disposable)
                        val runnable = {
                            if (!widget.project.isDisposed) {
                                DumbService.getInstance(widget.project).smartInvokeLater {
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
            if (!isShowing || widget.isDisposed || widget.project.isDisposed) {
                return
            }

            val id = TranslationPlugin.generateId("tooltip.new.engines.tts")
            val message = message("got.it.tooltip.text.new.engines")
            GotItTooltip(id, message, widget)
                .withHeader(message("got.it.tooltip.title.new.engines"))
                .show(this, GotItTooltip.TOP_MIDDLE)
        }

        private fun showPopup() {
            if (isLoadingTranslationEngines) {
                return
            }

            isLoadingTranslationEngines = true
            val componentRef = DisposableRef.create(widget, this)
            asyncLatch { latch ->
                runAsync {
                    latch.await()
                    TranslationEngineActionGroup()
                }
                    .successOnUiThread(componentRef) { component, group ->
                        val context = DataManager.getInstance().getDataContext(component)
                        val popup = group.createActionPopup(context)
                        val at = Point(0, -popup.content.preferredSize.height)
                        popup.show(RelativePoint(component, at))
                    }
                    .onError {
                        logger<TranslationWidget>().warn("Failed to show translation engines popup.", it)
                    }
                    .finishOnUiThread(componentRef, ModalityState.any()) { component, _ ->
                        component.isLoadingTranslationEngines = false
                    }
                    .disposeAfterProcessing(componentRef)
            }
        }
    }
}
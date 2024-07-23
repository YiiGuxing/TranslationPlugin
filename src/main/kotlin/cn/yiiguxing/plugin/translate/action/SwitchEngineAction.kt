package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.intellij.compat.action.UpdateInBackgroundCompatComboBoxAction
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.TranslateService
import cn.yiiguxing.plugin.translate.util.concurrent.*
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import org.jetbrains.concurrency.runAsync
import java.awt.event.ActionEvent
import javax.swing.JComponent


/**
 * Switch engine action
 */
class SwitchEngineAction : UpdateInBackgroundCompatComboBoxAction(), DumbAware, PopupAction {

    private var disposable: Disposable? = null

    @Volatile
    private var isActionPerforming = false

    @Volatile
    private var isButtonActionPerforming = false

    init {
        isEnabledInModalContext = true
        templatePresentation.text = message("action.SwitchEngineAction.text")
        templatePresentation.description = message("action.SwitchEngineAction.description")
    }

    private fun getDisposable(): Disposable {
        disposable?.let { Disposer.dispose(it) }
        return Disposer.newDisposable("${javaClass.name}#Disposable").also { disposable = it }
    }

    override fun update(e: AnActionEvent) {
        TranslateService.getInstance().translator.let { translator ->
            e.presentation.text = translator.name
            e.presentation.icon = if (isActionPerforming || isButtonActionPerforming) {
                AnimatedIcon.Default.INSTANCE
            } else {
                translator.icon
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (isActionPerforming) {
            return
        }

        isActionPerforming = true
        val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
        val expireDisposable = getDisposable()
        asyncLatch { latch ->
            runAsync {
                latch.await()
                TranslationEngineActionGroup()
            }
                .expireWith(expireDisposable)
                .successOnUiThread { group ->
                    if (isActionPerforming && !project.isDisposed) {
                        val dataContext = DataManager.getInstance().getDataContext(component)
                        group.createActionPopup(dataContext).showCenteredInCurrentWindow(project)
                    }
                }
                .finishOnUiThread(ModalityState.any()) { isActionPerforming = false }
        }
    }

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        throw UnsupportedOperationException()
    }

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton {
        return SwitchTranslationEngineComboBoxButton(presentation)
    }


    private inner class SwitchTranslationEngineComboBoxButton(presentation: Presentation) :
        ComboBoxButton(presentation) {
        private var actionGroup: TranslationEngineActionGroup? = null

        override fun createPopup(onDispose: Runnable?): JBPopup {
            return checkNotNull(actionGroup) { "Action group is null." }
                .createActionPopup(dataContext, null, onDispose)
        }

        override fun showPopup() {
            try {
                if (isButtonActionPerforming) {
                    super.showPopup()
                }
            } finally {
                actionGroup = null
                isButtonActionPerforming = false
            }
        }

        override fun fireActionPerformed(event: ActionEvent) {
            if (isButtonActionPerforming) {
                return
            }

            isButtonActionPerforming = true
            val expireDisposable = getDisposable()
            Disposer.register(expireDisposable) { isButtonActionPerforming = false }
            asyncLatch { latch ->
                runAsync {
                    latch.await()
                    TranslationEngineActionGroup()
                }
                    .expireWith(expireDisposable)
                    .successOnUiThread { group ->
                        if (isButtonActionPerforming && isShowing) {
                            actionGroup = group
                            super.fireActionPerformed(event)
                        } else {
                            isButtonActionPerforming = false
                        }
                    }
                    .errorOnUiThread(ModalityState.any()) { isButtonActionPerforming = false }
                    .finishOnUiThread(ModalityState.any()) { disposable = null }
            }
        }
    }
}
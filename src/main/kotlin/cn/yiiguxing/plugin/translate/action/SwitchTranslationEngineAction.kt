package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.TranslateService
import cn.yiiguxing.plugin.translate.util.concurrent.errorOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.expireWith
import cn.yiiguxing.plugin.translate.util.concurrent.finishOnUiThread
import cn.yiiguxing.plugin.translate.util.concurrent.successOnUiThread
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import org.jetbrains.concurrency.runAsync
import java.awt.event.ActionEvent
import javax.swing.JComponent


/**
 * Switch translator action
 */
class SwitchTranslationEngineAction : ComboBoxAction(), DumbAware, PopupAction {

    private var disposable: Disposable? = null

    @Volatile
    private var isActionPerforming = false

    @Volatile
    private var isButtonActionPerforming = false

    init {
        isEnabledInModalContext = true
        templatePresentation.text = message("action.SwitchTranslationEngineAction.text")
        templatePresentation.description = message("action.SwitchTranslationEngineAction.description")
    }

    private fun getDisposable(): Disposable {
        disposable?.let { Disposer.dispose(it) }
        return Disposer.newDisposable("${javaClass.name}#Disposable").also { disposable = it }
    }

    override fun update(e: AnActionEvent) {
        TranslateService.translator.let { translator ->
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
        val expireDisposable = getDisposable()
        runAsync { TranslationEngineActionGroup() }
            .expireWith(expireDisposable)
            .successOnUiThread { group ->
                if (isActionPerforming && !project.isDisposed) {
                    group.createActionPopup(e.dataContext).showCenteredInCurrentWindow(project)
                }
            }
            .finishOnUiThread(ModalityState.any()) { isActionPerforming = false }
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
            runAsync { TranslationEngineActionGroup() }
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
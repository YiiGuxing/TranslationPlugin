package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.util.TranslateService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.Dimension
import javax.swing.JComponent


/**
 * ChooseTranslatorAction
 *
 * Created by Yii.Guxing on 2018/01/08
 */
class ChooseTranslatorAction : ComboBoxAction(), DumbAware {

    init {
        setPopupTitle(TranslatorActionGroup.TITLE)
        isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
        TranslateService.translator.let {
            e.presentation.text = it.name
            e.presentation.icon = it.icon
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        WindowManagerEx.getInstanceEx()
                .findFrameFor(e.project)
                ?.component
                ?.let {
                    createActionPopup(TranslatorActionGroup.TITLE, e.dataContext, it).showInCenterOf(it)
                }
    }

    private fun createActionPopup(title: String?,
                                  context: DataContext,
                                  component: JComponent,
                                  disposeCallback: Runnable? = null): ListPopup {
        val group = createPopupActionGroup(component, context)
        return JBPopupFactory.getInstance()
                .createActionGroupPopup(title, group, context, false, shouldShowDisabledActions(),
                        false, disposeCallback, maxRows, preselectCondition)
                .apply { setMinimumSize(Dimension(minWidth, minHeight)) }
    }

    override fun createPopupActionGroup(button: JComponent)
            : DefaultActionGroup = DefaultActionGroup(TranslatorActionGroup.getActions())

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton =
            object : ComboBoxButton(presentation) {
                override fun createPopup(onDispose: Runnable?): JBPopup {
                    return createActionPopup(null, dataContext, this, onDispose)
                }
            }
}
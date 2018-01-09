package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.TranslateService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.Dimension
import javax.swing.JComponent

/**
 * ChooseTranslatorAction
 *
 * Created by Yii.Guxing on 2018/01/08
 */
class ChooseTranslatorAction : ComboBoxAction(), DumbAware {

    private val settings: Settings = Settings.instance
    private val translateService: TranslateService = TranslateService.instance

    init {
        setPopupTitle("Choose Translator")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = translateService.translator.name
    }

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        return DefaultActionGroup(translateService.getTranslators().map { translator ->
            object : DumbAwareAction(translator.name) {
                override fun actionPerformed(e: AnActionEvent) {
                    settings.translator = translator.id
                }
            }
        })
    }

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton =
            object : ComboBoxButton(presentation) {
                override fun createPopup(onDispose: Runnable?): JBPopup {
                    val context = dataContext
                    val group = createPopupActionGroup(this, context)
                    return JBPopupFactory.getInstance()
                            .createActionGroupPopup(null, group, context, false, shouldShowDisabledActions(),
                                    false, onDispose, maxRows, preselectCondition)
                            .apply { setMinimumSize(Dimension(minWidth, minHeight)) }
                }
            }
}
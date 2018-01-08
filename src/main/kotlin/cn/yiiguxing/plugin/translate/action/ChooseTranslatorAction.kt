package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.TranslateService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.JComponent

/**
 * ChooseTranslatorAction
 *
 * Created by Yii.Guxing on 2018/01/08
 */
class ChooseTranslatorAction : ComboBoxAction(), DumbAware {

    private val settings: Settings = Settings.instance
    private val translateService: TranslateService = TranslateService.instance

    override fun update(e: AnActionEvent) {
        e.presentation.text = translateService.translator.name
    }

    override fun createPopupActionGroup(button: JComponent): DefaultActionGroup {
        return DefaultActionGroup("Choose Translator", false).apply {
            addAll(translateService.getTranslators().map { translator ->
                object : DumbAwareAction(translator.name) {
                    override fun actionPerformed(e: AnActionEvent) {
                        settings.translator = translator.id
                    }
                }
            })
        }
    }
}
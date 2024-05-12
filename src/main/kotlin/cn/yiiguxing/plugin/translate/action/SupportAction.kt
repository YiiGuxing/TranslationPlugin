package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.SupportDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction
import icons.TranslationIcons

class SupportAction :
    DumbAwareAction({ message("support.notification") }, Presentation.NULL_STRING, TranslationIcons.Support) {
    override fun actionPerformed(e: AnActionEvent) = SupportDialog.show()
}
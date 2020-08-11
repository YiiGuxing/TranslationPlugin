package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class TranslatorWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return TranslatorWidget.ID
    }

    override fun getDisplayName(): String {
        return "Translator"
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        Disposer.dispose(widget)
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return TranslatorWidget(project)
    }

    override fun canBeEnabledOn(project: StatusBar): Boolean {
        return true
    }

}
package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class TranslationWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = TranslationWidget.ID

    override fun getDisplayName(): String = message("translation.widget.name")

    override fun isAvailable(project: Project): Boolean = true

    override fun canBeEnabledOn(project: StatusBar): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = TranslationWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)
}
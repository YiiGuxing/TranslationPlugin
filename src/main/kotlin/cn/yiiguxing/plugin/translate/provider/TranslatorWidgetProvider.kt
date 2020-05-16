package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.ui.TranslatorWidget
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.openapi.wm.impl.status.MemoryUsagePanel

class TranslatorWidgetProvider : StatusBarWidgetProvider {

    override fun getWidget(project: Project): StatusBarWidget = TranslatorWidget(project)

    override fun getAnchor(): String = "before ${MemoryUsagePanel.WIDGET_ID}"

}
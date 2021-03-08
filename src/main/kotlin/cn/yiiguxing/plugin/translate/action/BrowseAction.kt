package cn.yiiguxing.plugin.translate.action

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class BrowseAction(private val url: String) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(url)
    }
}
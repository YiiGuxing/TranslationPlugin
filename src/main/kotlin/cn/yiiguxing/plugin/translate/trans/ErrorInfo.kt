package cn.yiiguxing.plugin.translate.trans

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Translation error information.
 *
 * @property message the error message
 * @property continueActions the continue actions after the error
 */
data class ErrorInfo(
    val message: String,
    val continueActions: List<AnAction> = emptyList()
) {
    constructor(message: String, vararg continueActions: AnAction) : this(message, continueActions.toList())

    companion object {
        inline fun continueAction(name: String, crossinline action: (() -> Unit)): AnAction = object : AnAction(name) {
            override fun actionPerformed(e: AnActionEvent) = action()
        }

        fun openUrlAction(name: String, url: String): AnAction = object : AnAction(name) {
            override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(url)
        }
    }
}
package cn.yiiguxing.plugin.translate.trans

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

/**
 * Translation error information.
 *
 * @property message The error message
 * @property continueActions The continue actions after the error
 */
data class ErrorInfo(
    val message: String,
    val continueActions: List<AnAction> = emptyList()
) {
    constructor(message: String, vararg continueActions: AnAction) : this(message, continueActions.toList())

    companion object {

        /**
         * Creates a continue action.
         *
         * @param name The action name
         * @param description Describes current action
         * @param icon Action's icon
         */
        inline fun continueAction(
            name: String,
            description: String? = null,
            icon: Icon? = null,
            crossinline action: ((AnActionEvent) -> Unit)
        ): AnAction = object : DumbAwareAction(name, description, icon) {
            override fun actionPerformed(e: AnActionEvent) = action(e)
        }

        /**
         * Creates a continue actions to browse the specified [url].
         *
         * @param name The action name
         * @param url The url to browse
         * @param description Describes current action
         * @param icon Action's icon
         */
        fun browseUrlAction(
            name: String,
            url: String,
            description: String? = null,
            icon: Icon? = AllIcons.General.Web
        ): AnAction {
            return object : DumbAwareAction(name, description, icon) {
                override fun actionPerformed(e: AnActionEvent) = BrowserUtil.browse(url)
            }
        }
    }
}
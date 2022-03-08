package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.ReportSubmitter
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ErrorsDialog(
    project: Project?,
    private val message: String,
    private val throwable: Throwable
) : DialogWrapper(project, true) {

    private lateinit var commentArea: JBTextArea
    private lateinit var stacktraceArea: JBTextArea

    init {
        title = message("error.dialog.title")
        isModal = false
        init()
        setOKButtonText(message("error.report.to.yiiguxing.action"))
        setCancelButtonText(message("close.action.name"))
    }

    private fun getThrowableText(): String {
        return IdeaLogger.getThrowableRenderer().doRender(throwable).joinToString("\n")
    }

    override fun createCenterPanel(): JComponent {
        commentArea = JBTextArea(5, 0).apply {
            margin = JBUI.insets(2)
            emptyText.text = message("error.dialog.comment.hint")
        }
        stacktraceArea = JBTextArea(5, 0).apply {
            margin = JBUI.insets(2)
            text = getThrowableText()
            caretPosition = 0
        }

        val commentPanel = commentArea.wrapWithScrollPane()

        val stacktracePanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.emptyTop(8)

            val label = JBLabel(message("error.dialog.label.stacktrace")).apply { border = JBUI.Borders.emptyBottom(2) }
            add(label, BorderLayout.NORTH)

            add(stacktraceArea.wrapWithScrollPane(600, 350), BorderLayout.CENTER)
        }

        return JPanel(BorderLayout()).apply {
            preferredSize = JBUI.size(800, 400)
            minimumSize = JBUI.size(680, 400)
            add(commentPanel, BorderLayout.NORTH)
            add(stacktracePanel, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        val comment = commentArea.text?.trim() ?: ""
        val stacktrace = stacktraceArea.text?.trim() ?: ""
        ReportSubmitter.submit(message, comment, stacktrace)

        close(OK_EXIT_CODE)
    }

    companion object {
        fun show(project: Project?, message: String, throwable: Throwable) {
            ErrorsDialog(project, message, throwable).show()
        }

        private fun JComponent.wrapWithScrollPane(width: Int = 0, height: Int = 0) =
            JBScrollPane(this).apply {
                if (width > 0 && height > 0) {
                    minimumSize = JBUI.size(width, height)
                }
            }
    }
}
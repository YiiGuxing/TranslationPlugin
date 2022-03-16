package cn.yiiguxing.plugin.translate.diagnostic

import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.spanX
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.BrowserLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBDimension
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane

internal class VerificationDialog(
    project: Project?,
    parentComponent: Component,
    private val verification: GitHubVerification
) :
    DialogWrapper(project, parentComponent, false, IdeModalityType.IDE) {

    init {
        title = "Sign in to GitHub"
        isModal = true
        isResizable = false
        init()
    }

    override fun createActions(): Array<Action> {
        return arrayOf(CopyAndOpenAction(), okAction.apply { putValue(DEFAULT_ACTION, null) }, cancelAction)
    }

    override fun createCenterPanel(): JComponent {
        val gap = "${JBUIScale.scale(8)}!"
        val layoutConstraints = LC()
            .gridGap(gap, gap)
            .insets("0")
        return JPanel(MigLayout(layoutConstraints)).apply {
            preferredSize = JBDimension(350, -1)

            val text = JTextPane().apply {
                isEditable = false
                text = "Error Reporter uses a GitHub account. Please enter the following code " +
                        "on the GitHub website to authorize your GitHub account with Error Reporter."
            }
            add(text, spanX().gapBottom(JBUIScale.scale(4).toString()).wrap())

            add(JBLabel("GitHub website:"))
            add(BrowserLink(verification.verificationUri), fillX().wrap())
            add(JBLabel("Device code:"))

            val userCodeTextField = ExtendableTextField(verification.userCode).apply {
                isEditable = false
                addExtension(ExtendableTextComponent.Extension.create(AllIcons.Actions.Copy, "Copy") {
                    copyUserCode()
                })
            }
            add(userCodeTextField, fillX().wrap())
        }
    }

    private fun copyUserCode() {
        CopyPasteManager.getInstance().setContents(StringSelection(verification.userCode))
    }

    private inner class CopyAndOpenAction : DialogWrapperAction("Copy and Open") {

        init {
            putValue(DEFAULT_ACTION, true)
        }

        override fun doAction(e: ActionEvent) {
            copyUserCode()
            BrowserUtil.browse(verification.verificationUri)
            close(OK_EXIT_CODE)
        }
    }

}
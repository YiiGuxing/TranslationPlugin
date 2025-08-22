package cn.yiiguxing.plugin.translate.ui.notification.banner

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

class EditorBannerProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> = Function { fileEditor ->
        (fileEditor as? TextEditor)?.editor?.let { editor ->
            EditorBannerManager.getEditorBanner(editor)?.let { banner ->
                createBannerComponent(fileEditor, editor, banner)
            }
        }
    }

    private fun createBannerComponent(
        fileEditor: FileEditor,
        editor: Editor,
        banner: EditorBanner
    ): JComponent {
        val status = when (banner.status) {
            EditorBanner.Status.INFO -> EditorNotificationPanel.Status.Info
            EditorBanner.Status.SUCCESS -> EditorNotificationPanel.Status.Success
            EditorBanner.Status.WARNING -> EditorNotificationPanel.Status.Warning
            EditorBanner.Status.ERROR -> EditorNotificationPanel.Status.Error
            EditorBanner.Status.PROMO -> EditorNotificationPanel.Status.Promo
        }
        return EditorNotificationPanel(fileEditor, status).apply {
            text = banner.message

            banner.actions.forEach { action ->
                createActionLabel(action.text) {
                    action.runnable.run()
                }.apply {
                    icon = action.icon
                }
            }

            if (banner.showCloseButton) {
                setCloseAction { EditorBannerManager.setEditorBanner(editor, null) }
            }
        }
    }
}
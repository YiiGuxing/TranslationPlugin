package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.config.OpenAiRequestConfigService
import cn.yiiguxing.plugin.translate.util.Http
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.json.JsonFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

/**
 * Dialog for editing the OpenAI request config file
 * with the IDE's editor capabilities (e.g. JSON schema validation).
 *
 * The config files must be prepared by
 * [OpenAiRequestConfigService.prepareConfigFilesForEditing]
 * before creating this dialog.
 */
class OpenAiConfigFileEditorDialog(private val project: Project) : DialogWrapper(project) {

    private companion object {
        val LOG = logger<OpenAiConfigFileEditorDialog>()
    }

    private val virtualFile: VirtualFile
    private val editor: Editor
    private val originalConfig: String

    init {
        isResizable = true
        title = message("openai.config.editor.dialog.title")

        val configFile = OpenAiRequestConfigService.CONFIG_FILE
        virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByIoFile(configFile.toFile())
            ?: throw IllegalStateException("Failed to find the OpenAI request config file: $configFile")
        virtualFile.isWritable = true
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            ?: throw IllegalStateException("Failed to open the OpenAI request config file: $configFile")
        originalConfig = document.text
        editor = EditorFactory.getInstance().createEditor(document, project, JsonFileType.INSTANCE, false)

        init()
    }

    override fun createCenterPanel(): JComponent = editor.component.apply {
        preferredSize = JBUI.size(720, 560)
    }

    override fun createLeftSideActions(): Array<Action> {
        val openInEditorAction = object : AbstractAction(
            message("openai.config.editor.dialog.action.open.in.editor"),
            AllIcons.FileTypes.Config
        ) {
            override fun actionPerformed(e: ActionEvent?) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                Messages.showInfoMessage(
                    project,
                    message("openai.config.editor.dialog.message.open.in.editor"),
                    message("openai.config.editor.dialog.title")
                )
            }
        }
        val openConfigDirectoryAction = object : AbstractAction(
            message("openai.config.editor.dialog.action.open.config.directory"),
            AllIcons.Nodes.Folder
        ) {
            override fun actionPerformed(e: ActionEvent?) {
                try {
                    BrowserUtil.browse(OpenAiRequestConfigService.CONFIG_DIRECTORY.toFile())
                } catch (exception: Exception) {
                    LOG.warn("Failed to open the OpenAI request config directory.", exception)
                    Messages.showErrorDialog(
                        editor.component,
                        message("openai.config.editor.dialog.error.open.config.directory"),
                        message("error.title")
                    )
                }
            }
        }
        return arrayOf(openInEditorAction, openConfigDirectoryAction)
    }

    override fun doOKAction() {
        val text = editor.document.text
        if (!text.isValidJsonObject()) {
            Messages.showErrorDialog(
                editor.component,
                message("openai.config.editor.dialog.error.invalid.json"),
                message("error.title")
            )
            return
        }

        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            FileDocumentManager.getInstance().saveDocument(editor.document)
        }
        super.doOKAction()
    }

    override fun doCancelAction() {
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            editor.document.setText(originalConfig)
            FileDocumentManager.getInstance().saveDocument(editor.document)
        }

        super.doCancelAction()
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
        super.dispose()
    }

    private fun String.isValidJsonObject(): Boolean {
        return try {
            Http.defaultGson.fromJson(this, JsonObject::class.java)
            true
        } catch (_: JsonParseException) {
            false
        }
    }
}

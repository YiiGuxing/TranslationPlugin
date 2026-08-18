package cn.yiiguxing.plugin.translate.trans.openai.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.openai.config.OpenAiRequestConfigService
import cn.yiiguxing.plugin.translate.util.Http
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.intellij.json.JsonFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.ui.JBUI
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

    private val editor: Editor
    private val originalConfig: String

    init {
        isResizable = true
        title = message("openai.config.editor.dialog.title")

        val configFile = OpenAiRequestConfigService.CONFIG_FILE
        val virtualFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByIoFile(configFile.toFile())
            ?: throw IllegalStateException("Failed to find the OpenAI request config file: $configFile")
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            ?: throw IllegalStateException("Failed to open the OpenAI request config file: $configFile")
        originalConfig = document.text
        editor = EditorFactory.getInstance().createEditor(document, project, JsonFileType.INSTANCE, false)

        init()
    }

    override fun createCenterPanel(): JComponent = editor.component.apply {
        preferredSize = JBUI.size(720, 560)
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

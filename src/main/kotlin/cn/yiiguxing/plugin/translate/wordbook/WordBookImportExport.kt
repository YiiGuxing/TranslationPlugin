package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


private const val EXPORT_NOTIFICATIONS_ID = "Word Book Export"

private const val EXTENSION_XML = "xml"
private const val EXTENSION_JSON = "json"

fun WordBookExporter.export(project: Project?, words: List<WordBookItem>) {
    val targetFile = FileChooserFactory
        .getInstance()
        .createSaveFileDialog(
            FileSaverDescriptor(
                message("wordbook.window.export.ui.file.chooser.title"),
                message("wordbook.window.export.ui.file.chooser.message"),
                extension
            ),
            project
        )
        .save(null, "wordbook.$extension")
        ?.getVirtualFile(true)

    val title = message("wordbook.window.export.notification.title")
    if (targetFile != null) {
        try {
            val wordList = words.sortedBy { it.id ?: Long.MAX_VALUE }
            WriteAction.run<Throwable> {
                targetFile.getOutputStream(this).use {
                    export(wordList, it)
                }
            }
        } catch (e: Throwable) {
            Notifications.showErrorNotification(
                project,
                EXPORT_NOTIFICATIONS_ID,
                title,
                message("wordbook.window.export.notification.failed", e.message ?: ""),
                e
            )
            return
        }

        Notifications.showNotification(
            EXPORT_NOTIFICATIONS_ID,
            title,
            message("wordbook.window.export.notification.exported.message", targetFile.presentableUrl),
            NotificationType.INFORMATION,
            project
        )
    } else {
        Notifications.showNotification(
            EXPORT_NOTIFICATIONS_ID,
            title,
            message("wordbook.window.export.notification.cannot.write.message"),
            NotificationType.ERROR,
            project
        )
    }
}

fun importWordBook(project: Project?) {
    val selectFile = selectImportSource(project) ?: return

    when {
        EXTENSION_XML.equals(selectFile.extension, true) -> {

        }
        EXTENSION_JSON.equals(selectFile.extension, true) -> {

        }
        else -> {

        }
    }
}

private fun selectImportSource(project: Project?): VirtualFile? {
    val descriptor = object : FileChooserDescriptor(true, false, false, false, false, false) {
        override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean {
            return (file.isDirectory || file.extension.let { ext ->
                EXTENSION_XML.equals(ext, true) || EXTENSION_JSON.equals(ext, true)
            }) && (showHiddenFiles || !FileElement.isFileHidden(file))
        }

        override fun isFileSelectable(file: VirtualFile): Boolean {
            return !file.isDirectory && file.extension.let { ext ->
                EXTENSION_XML.equals(ext, true) || EXTENSION_JSON.equals(ext, true)
            }
        }
    }

    return FileChooserFactory.getInstance()
        .createFileChooser(descriptor, project, null)
        .choose(project)
        .firstOrNull()
        ?.apply { refresh(false, false) }
}
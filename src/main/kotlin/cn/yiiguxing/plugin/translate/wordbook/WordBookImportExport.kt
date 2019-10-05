package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project


private const val EXPORT_NOTIFICATIONS_ID = "Word Book Export"

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

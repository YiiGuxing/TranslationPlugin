package cn.yiiguxing.plugin.translate.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.e
import cn.yiiguxing.plugin.translate.util.w
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileElement
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


/**
 *                                                       █████
 *                                                      ░░███
 *     ██████  █████ █████ ████████   ██████  ████████  ███████    ██████  ████████   █████
 *    ███░░███░░███ ░░███ ░░███░░███ ███░░███░░███░░███░░░███░    ███░░███░░███░░███ ███░░
 *   ░███████  ░░░█████░   ░███ ░███░███ ░███ ░███ ░░░   ░███    ░███████  ░███ ░░░ ░░█████
 *   ░███░░░    ███░░░███  ░███ ░███░███ ░███ ░███       ░███ ███░███░░░   ░███      ░░░░███
 *   ░░██████  █████ █████ ░███████ ░░██████  █████      ░░█████ ░░██████  █████     ██████
 *    ░░░░░░  ░░░░░ ░░░░░  ░███░░░   ░░░░░░  ░░░░░        ░░░░░   ░░░░░░  ░░░░░     ░░░░░░
 *                         ░███
 *                         █████
 *                        ░░░░░
 */
val WORD_BOOK_EXPORTERS: List<WordBookExporter> = listOf(
    JsonWordBookExporter(),
    XmlWordBookExporter(),
    YoudaoXmlWordBookExporter(),
    TxtWordBookExporter()
)

private const val EXTENSION_XML = "xml"
private const val EXTENSION_JSON = "json"

/**
 *     ███                                                █████
 *    ░░░                                                ░░███
 *    ████  █████████████   ████████   ██████  ████████  ███████    ██████  ████████   █████
 *   ░░███ ░░███░░███░░███ ░░███░░███ ███░░███░░███░░███░░░███░    ███░░███░░███░░███ ███░░
 *    ░███  ░███ ░███ ░███  ░███ ░███░███ ░███ ░███ ░░░   ░███    ░███████  ░███ ░░░ ░░█████
 *    ░███  ░███ ░███ ░███  ░███ ░███░███ ░███ ░███       ░███ ███░███░░░   ░███      ░░░░███
 *    █████ █████░███ █████ ░███████ ░░██████  █████      ░░█████ ░░██████  █████     ██████
 *   ░░░░░ ░░░░░ ░░░ ░░░░░  ░███░░░   ░░░░░░  ░░░░░        ░░░░░   ░░░░░░  ░░░░░     ░░░░░░
 *                          ░███
 *                          █████
 *                         ░░░░░
 */
val WORD_BOOK_IMPORTERS: Map<String, WordBookImporter> = mapOf(
    EXTENSION_JSON to JsonWordBookImporter(),
    EXTENSION_XML to XmlWordBookImporter()
)


private const val EXPORT_NOTIFICATIONS_ID = "Word Book Export"
private const val IMPORT_NOTIFICATIONS_ID = "Word Book Import"

private val LOG = Logger.getInstance("#WordBookImportExport")


fun WordBookExporter.export(project: Project?, words: List<WordBookItem>) {
    val targetFileWrapper = FileChooserFactory
        .getInstance()
        .createSaveFileDialog(
            FileSaverDescriptor(
                message("wordbook.window.ui.file.chooser.title"),
                "",
                extension
            ),
            project
        )
        .save(null, "wordbook.$extension") ?: return

    val targetFile = targetFileWrapper.getVirtualFile(true)

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

        Notifications.showInfoNotification(
            EXPORT_NOTIFICATIONS_ID,
            title,
            message("wordbook.window.export.notification.exported.message", targetFile.presentableUrl),
            project
        )
    } else {
        Notifications.showErrorNotification(
            EXPORT_NOTIFICATIONS_ID,
            title,
            message("wordbook.window.export.notification.cannot.write.message"),
            project
        )
    }
}

fun importWordBook(project: Project?) {
    val selectFile = selectImportSource(project) ?: return
    val title = message("wordbook.window.import.notification.title")

    val importer = WORD_BOOK_IMPORTERS[selectFile.extension?.toLowerCase()]
    if (importer == null) {
        LOG.e("Word book import: file extension=${selectFile.extension}")
        Notifications.showErrorNotification(
            IMPORT_NOTIFICATIONS_ID,
            title,
            message("wordbook.window.import.notification.cannot.import"),
            project
        )
        return
    }

    ProgressManager.getInstance().run(object : Task.Backgroundable(project, message("word.book.progress.importing"), true) {
        override fun run(indicator: ProgressIndicator) {
            selectFile.inputStream.use { importer.import(it, indicator) }
        }

        override fun onSuccess() {
            Notifications.showInfoNotification(
                IMPORT_NOTIFICATIONS_ID,
                title,
                message("wordbook.window.import.notification.imported.message"),
                project
            )
        }

        override fun onThrowable(error: Throwable) {
            LOG.w("Word book import", error)
            Notifications.showErrorNotification(
                IMPORT_NOTIFICATIONS_ID,
                title,
                message("wordbook.window.import.notification.cannot.import"),
                project
            )
        }
    })
}

private fun VirtualFile.isValidExtension(): Boolean {
    val extension = extension ?: return false
    return WORD_BOOK_IMPORTERS.containsKey(extension.toLowerCase())
}

private fun selectImportSource(project: Project?): VirtualFile? {
    val descriptor = object : FileChooserDescriptor(true, false, false, false, false, false) {
        override fun isFileVisible(file: VirtualFile, showHiddenFiles: Boolean): Boolean {
            return (file.isDirectory || file.isValidExtension()) && (showHiddenFiles || !FileElement.isFileHidden(file))
        }

        override fun isFileSelectable(file: VirtualFile): Boolean {
            return !file.isDirectory && file.isValidExtension()
        }
    }

    descriptor.title = message("wordbook.window.ui.file.chooser.title")

    return FileChooserFactory.getInstance()
        .createFileChooser(descriptor, project, null)
        .choose(project)
        .firstOrNull()
        ?.apply { refresh(false, false) }
}
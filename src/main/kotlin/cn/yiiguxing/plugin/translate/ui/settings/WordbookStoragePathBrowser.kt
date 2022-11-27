package cn.yiiguxing.plugin.translate.ui.settings

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.TranslationStorages
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookService
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.runAsync
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Suppress("UnstableApiUsage")
internal class WordbookStoragePathBrowser(val settings: Settings) : TextBrowseFolderListener(
    FileChooserDescriptor(false, true, false, false, false, false)
        .withTitle(message("settings.wordbook.title.select.storage.path"))
        .withDescription(message("settings.wordbook.label.storage.path.tips"))
) {

    override fun onFileChosen(chosenFile: VirtualFile) {
        super.onFileChosen(chosenFile)

        val newStoragePath = componentText?.takeIf { it.isNotEmpty() }
        setNewWordbookStorage(settings, newStoragePath)
    }

    companion object {
        private fun String?.toPath(): Path {
            return this?.trim()?.takeIf { it.isNotEmpty() }?.let { Paths.get(it) } ?: TranslationStorages.DATA_DIRECTORY
        }

        fun restoreDefaultWordbookStorage(settings: Settings) {
            setNewWordbookStorage(settings, null)
        }

        private fun setNewWordbookStorage(settings: Settings, newStoragePath: String?) {
            val oldPath = settings.wordbookStoragePath.toPath()
            val newPath = newStoragePath.toPath()
            if (newPath == oldPath) {
                return
            }

            val oldStorageFile = WordBookService.getStorageFile(oldPath)
            if (!Files.exists(oldStorageFile)) {
                settings.wordbookStoragePath = newStoragePath
                return
            }

            val title = message("settings.wordbook.alert.title.move.storage.file")
            val move = MessageDialogBuilder
                .yesNo(title, message("settings.wordbook.alert.message.move.storage.file"))
                .icon(UIUtil.getQuestionIcon())
                .ask(null as Project?)
            if (!move) {
                settings.wordbookStoragePath = newStoragePath
                return
            }

            val newStorageFile = WordBookService.getStorageFile(newPath)
            if (!Files.exists(newStorageFile)) {
                moveWordbookStorageFile(oldStorageFile, newStorageFile) {
                    settings.wordbookStoragePath = newStoragePath
                }
                return
            }

            val overwrite = MessageDialogBuilder
                .okCancel(title, message("settings.wordbook.alert.message.storage.file.already.exists"))
                .icon(UIUtil.getWarningIcon())
                .yesText(message("overwrite.action.name"))
                .noText(message("skip.action.name"))
                .ask(null as Project?)
            if (overwrite) {
                moveWordbookStorageFile(oldStorageFile, newStorageFile) {
                    settings.wordbookStoragePath = newStoragePath
                }
            } else {
                settings.wordbookStoragePath = newStoragePath
            }
        }

        private fun moveWordbookStorageFile(fromPath: Path, toPath: Path, onSuccess: () -> Unit) {
            if (fromPath == toPath) {
                return
            }

            runAsync {
                Files.createDirectories(toPath.parent)
                Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING)
            }.onError {
                invokeLater {
                    val retry = MessageDialogBuilder
                        .okCancel(
                            message("settings.wordbook.alert.title.move.storage.file"),
                            message("settings.wordbook.alert.message.failed.to.move.storage.file", it.message ?: "")
                        )
                        .icon(UIUtil.getErrorIcon())
                        .ask(null as Project?)
                    if (retry) {
                        moveWordbookStorageFile(fromPath, toPath, onSuccess)
                    }
                }
            }.onSuccess {
                invokeLater(onSuccess)
            }
        }
    }

}
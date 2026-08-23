package cn.yiiguxing.plugin.translate.trans.openai.config

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import icons.TranslationIcons
import javax.swing.Icon


private fun VirtualFile.isOpenAiRequestConfigFile(): Boolean =
    isInLocalFileSystem && toNioPath() == OpenAiRequestConfigService.CONFIG_FILE


class OpenAiRequestConfigEditorTabTitleProvider : EditorTabTitleProvider {
    override fun getEditorTabTitle(
        project: Project,
        file: VirtualFile
    ): @NlsContexts.TabTitle String? {
        return if (file.isOpenAiRequestConfigFile()) {
            "OpenAi Request Config"
        } else null
    }
}

class OpenAiRequestConfigFileIconProvider : FileIconProvider {
    override fun getIcon(
        file: VirtualFile,
        flags: Int,
        project: Project?
    ): Icon? {
        return if (file.isOpenAiRequestConfigFile()) {
            TranslationIcons.Engines.OpenAI
        } else null
    }
}
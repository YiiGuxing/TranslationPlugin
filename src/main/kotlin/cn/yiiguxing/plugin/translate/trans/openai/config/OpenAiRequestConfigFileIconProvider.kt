package cn.yiiguxing.plugin.translate.trans.openai.config

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import icons.TranslationIcons
import java.nio.file.Paths
import javax.swing.Icon

class OpenAiRequestConfigFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
        return if (Paths.get(file.path) == OpenAiRequestConfigService.CONFIG_FILE) {
            TranslationIcons.Engines.OpenAI
        } else null
    }
}
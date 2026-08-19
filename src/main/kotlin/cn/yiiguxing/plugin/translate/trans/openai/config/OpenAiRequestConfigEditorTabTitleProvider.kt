package cn.yiiguxing.plugin.translate.trans.openai.config

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

class OpenAiRequestConfigEditorTabTitleProvider : EditorTabTitleProvider {
    override fun getEditorTabTitle(project: Project, file: VirtualFile): @NlsContexts.TabTitle String? {
        return if (Paths.get(file.path) == OpenAiRequestConfigService.CONFIG_FILE) {
            "OpenAi Request Config"
        } else null
    }
}
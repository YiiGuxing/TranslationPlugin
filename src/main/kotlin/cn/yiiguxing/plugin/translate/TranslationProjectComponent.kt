package cn.yiiguxing.plugin.translate

import cn.yiiguxing.plugin.translate.util.TranslationUIManager
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project

class TranslationProjectComponent(project: Project) : AbstractProjectComponent(project) {

    override fun projectOpened() {
    }

    override fun disposeComponent() {
        TranslationUIManager.disposeUI(myProject)
    }

}

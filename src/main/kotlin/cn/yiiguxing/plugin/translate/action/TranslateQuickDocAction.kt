package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.TranslationManager
import cn.yiiguxing.plugin.translate.util.splitWords
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * TranslateQuickDocAction
 *
 * Created by Yii.Guxing on 2017/9/11
 */
class TranslateQuickDocAction : AnAction(), DumbAware, HintManagerImpl.ActionToIgnore {

    init {
        isEnabledInModalContext = true
    }

    override fun update(e: AnActionEvent) {
        val selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)
        e.presentation.isEnabled = !selected.isNullOrBlank()
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT)
                ?.splitWords()
                ?.let {
                    e.project.let { project ->
                        project?.hideDocInfoHint()
                        TranslationManager.instance.showDialog(project).translate(it)
                    }
                }
    }

    private companion object {
        fun Project.hideDocInfoHint() {
            DocumentationManager.getInstance(this).docInfoHint?.cancel()
        }
    }
}
package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.action.TranslateRenderedDocSelectionAction
import com.intellij.codeInsight.documentation.DocumentationActionProvider
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase

class TranslateDocumentationActionProvider : DocumentationActionProvider {
    override fun additionalActions(
        editor: Editor,
        docComment: PsiDocCommentBase?,
        renderedText: String?
    ): List<AnAction> {
        val actions: MutableList<AnAction> = mutableListOf(
            Separator(),
            TranslateRenderedDocSelectionAction(),
        )
        if (docComment != null) {
            actions.add(TranslateRenderedDocAction(editor, docComment))
        }
        return actions
    }
}


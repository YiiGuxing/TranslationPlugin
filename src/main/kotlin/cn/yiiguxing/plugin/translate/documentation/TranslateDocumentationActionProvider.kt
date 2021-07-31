package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.action.ToggleQuickDocTranslationAction
import cn.yiiguxing.plugin.translate.action.TranslateQuickDocSelectionAction
import cn.yiiguxing.plugin.translate.action.TranslateRenderedDocSelectionAction
import com.intellij.codeInsight.documentation.DocumentationActionProvider
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase

class TranslateDocumentationActionProvider : DocumentationActionProvider {
    override fun additionalActions(
        editor: Editor,
        docComment: PsiDocCommentBase,
        renderedText: String
    ): List<AnAction> {
        return listOf(
            Separator(),
            TranslateRenderedDocSelectionAction(),
            TranslateRenderedDocAction(editor, docComment)
        )
    }

    override fun additionalActions(component: DocumentationComponent): List<AnAction> {
        return listOf(
            Separator(),
            TranslateQuickDocSelectionAction(),
            ToggleQuickDocTranslationAction()
        )
    }

}


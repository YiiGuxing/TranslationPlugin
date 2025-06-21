package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.actions.ToggleQuickDocTranslationAction
import cn.yiiguxing.plugin.translate.documentation.actions.TranslateQuickDocSelectionAction
import cn.yiiguxing.plugin.translate.documentation.actions.TranslateRenderedDocAction
import cn.yiiguxing.plugin.translate.documentation.actions.TranslateRenderedDocSelectionAction
import com.intellij.codeInsight.documentation.DocumentationActionProvider
import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocCommentBase
import org.jetbrains.annotations.ApiStatus

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

    // TODO: Remove in v4.0
    @ApiStatus.ScheduledForRemoval(inVersion = "4.0")
    @Deprecated("Will be removed in v4.0")
    override fun additionalActions(component: DocumentationComponent): List<AnAction> {
        return listOf(
            Separator(),
            TranslateQuickDocSelectionAction(),
            ToggleQuickDocTranslationAction()
        )
    }
}


package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext

class TranslationPromoter : ActionPromoter {

    override fun promote(actions: MutableList<out AnAction>, context: DataContext): MutableList<AnAction> {
        return ArrayList(actions).apply { sortWith(COMPARATOR) }
    }

    companion object {
        private val COMPARATOR = Comparator.comparingInt { action: AnAction ->
            when (action) {
                is TranslateRenderedDocSelectionAction -> 0
                is PinBalloonAction,
                is EditorTranslateAction,
                is ToggleQuickDocTranslationAction -> 1
                else -> 2
            }
        }
    }
}
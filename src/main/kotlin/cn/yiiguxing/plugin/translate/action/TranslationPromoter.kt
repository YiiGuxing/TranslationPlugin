package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import java.util.*

class TranslationPromoter : ActionPromoter {
    override fun promote(actions: MutableList<AnAction>, context: DataContext): MutableList<AnAction> {
        val newList: MutableList<AnAction> = ArrayList(actions)
        val comparator = Comparator.comparingInt { action: AnAction ->
            when (action) {
                is PinBalloonAction,
                is EditorTranslateAction,
                is ToggleQuickDocTranslationAction -> 0
                else -> 1
            }
        }
        newList.sortWith(comparator)
        return newList
    }
}
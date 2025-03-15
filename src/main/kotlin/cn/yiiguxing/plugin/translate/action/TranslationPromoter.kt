package cn.yiiguxing.plugin.translate.action

import com.intellij.openapi.actionSystem.ActionPromoter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext

private val COMPARATOR = Comparator.comparingInt { action: AnAction ->
    when (action) {
        is ImportantTranslationAction -> -action.priority.also { checkActionPriority(it) }
        else -> Int.MAX_VALUE
    }
}

class TranslationPromoter : ActionPromoter {

    override fun promote(actions: MutableList<out AnAction?>, context: DataContext): MutableList<AnAction> {
        return actions.asSequence().filterNotNull().sortedWith(COMPARATOR).toMutableList()
    }

}
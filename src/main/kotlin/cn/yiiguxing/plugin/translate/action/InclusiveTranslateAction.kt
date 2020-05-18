package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.SelectionMode

/**
 * InclusiveTranslateAction
 */
class InclusiveTranslateAction : TranslateAction(false) {

    init {
        templatePresentation.description = message("action.description.inclusive")
    }

    override val selectionMode: SelectionMode
        get() = SelectionMode.INCLUSIVE

}
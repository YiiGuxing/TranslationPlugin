package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.util.SelectionMode

/**
 * InclusiveTranslateAction
 *
 * Created by Yii.Guxing on 2018/2/7
 */
class InclusiveTranslateAction : TranslateAction(false) {

    init {
        @Suppress("InvalidBundleOrProperty")
        templatePresentation.description = message("action.description.inclusive")
    }

    override val selectionMode: SelectionMode
        get() = SelectionMode.INCLUSIVE

}
package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.Settings

/**
 * 翻译动作，自动从最大范围内取词，优先选择
 */
class EditorTranslateAction : TranslateAction(true) {

    private val mSettings: Settings = Settings.getInstance()

    override val selectionMode
        get() = mSettings.autoSelectionMode
}

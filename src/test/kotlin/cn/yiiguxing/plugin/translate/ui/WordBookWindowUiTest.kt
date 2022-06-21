package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.wordbook.WordBookWindowComponent
import cn.yiiguxing.plugin.translate.util.ObservableValue
import cn.yiiguxing.plugin.translate.wordbook.WordBookState


fun main() = uiTest("Word Book Window Ui Test", 400, 800/*, true*/) {
    WordBookWindowComponent().apply { bindState(ObservableValue(WordBookState.INITIALIZATION_ERROR)) }.component
}
package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.form.WordBookWindowForm
import java.awt.CardLayout

/**
 * WordBookPanel
 *
 * Created by Yii.Guxing on 2019/09/03.
 */
class WordBookPanel() : WordBookWindowForm() {

    fun showMessagePane() {
        (root.layout as CardLayout).show(root, CARD_MESSAGE)
    }

    fun showTable() {
        (root.layout as CardLayout).show(root, CARD_TABLE)
    }

    companion object {
        private const val CARD_MESSAGE = "CARD_MESSAGE"
        private const val CARD_TABLE = "CARD_TABLE"
    }
}
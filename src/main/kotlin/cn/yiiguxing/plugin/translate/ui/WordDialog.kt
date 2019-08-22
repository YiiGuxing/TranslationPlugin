@file:Suppress("InvalidBundleOrProperty")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordDialogForm
import cn.yiiguxing.plugin.translate.util.invokeLater
import cn.yiiguxing.plugin.translate.wordbook.WordBookItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import javax.swing.Action

/**
 * Word of the day dialog
 *
 * Created by Yii.Guxing on 2019/08/20.
 */
class WordDialog(project: Project?, wordBookItem: WordBookItem) : WordDialogForm(project) {

    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)

    init {
        isModal = false
        title = message("wordOfTheDay.title")
        horizontalStretch = 1.33f
        verticalStretch = 1.25f
        setOKButtonText(message("wordOfTheDay.button.next"))
        setCancelButtonText(message("wordOfTheDay.button.close"))

        Disposer.register(disposable, ttsButton)
        init()
    }

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction, cancelAction)

    fun setWord(word: WordBookItem) {
        wordView.text = word.word
        phoneticLabel.text = word.phonetic
        phoneticLabel.isVisible = !word.phonetic.isNullOrBlank()
        ttsButton.dataSource { word.word to word.sourceLanguage }
    }

    override fun show() {
        super.show()
        invokeLater { focusManager.requestFocus(window, true) }
    }

}
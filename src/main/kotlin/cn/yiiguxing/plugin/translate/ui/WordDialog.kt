@file:Suppress("InvalidBundleOrProperty")

package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.form.WordDialogForm
import com.intellij.openapi.project.Project
import javax.swing.Action

/**
 * Word of the day dialog
 *
 * Created by Yii.Guxing on 2019/08/20.
 */
class WordDialog(project: Project?) : WordDialogForm(project) {

    init {
        isModal = false
        title = message("wordOfTheDay.title")
        wordView.text = "Word"
        horizontalStretch = 1.33f
        verticalStretch = 1.25f
        setOKButtonText(message("wordOfTheDay.button.next"))
        setCancelButtonText(message("wordOfTheDay.button.close"))

        init()
    }

    override fun getStyle(): DialogStyle = DialogStyle.COMPACT

    override fun createActions(): Array<Action> = arrayOf(okAction, cancelAction)

}
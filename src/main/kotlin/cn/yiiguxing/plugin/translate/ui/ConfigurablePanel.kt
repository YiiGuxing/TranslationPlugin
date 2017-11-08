package cn.yiiguxing.plugin.translate.ui

import javax.swing.JComponent

/**
 * ConfigurablePanel
 *
 *
 * Created by Yii.Guxing on 2017/11/8
 */
interface ConfigurablePanel {

    val component: JComponent

    val isModified: Boolean

    fun reset()

    fun apply()

    interface TranslateApiPanel : ConfigurablePanel {
        val id: String

        val title: String
    }

}

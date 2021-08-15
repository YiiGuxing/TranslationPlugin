package cn.yiiguxing.plugin.translate.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

interface TranslationDialogUiProvider {
    fun createPinButton(): JComponent
    fun createSettingsButton(): JComponent

    companion object {
        fun testProvider(): TranslationDialogUiProvider = TestProvider()

        private class TestProvider : TranslationDialogUiProvider {

            override fun createPinButton(): JComponent = actionButtonLike(AllIcons.General.Pin_tab)

            override fun createSettingsButton(): JComponent = actionButtonLike(AllIcons.General.GearPlain)

            private fun actionButtonLike(icon: Icon): JComponent {
                return JPanel().apply {
                    minimumSize = ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
                    add(JLabel().apply { this.icon = icon })
                }
            }
        }
    }
}
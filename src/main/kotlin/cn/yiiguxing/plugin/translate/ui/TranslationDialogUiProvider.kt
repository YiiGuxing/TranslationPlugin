package cn.yiiguxing.plugin.translate.ui

import javax.swing.JComponent

interface TranslationDialogUiProvider {
    fun createPinButton(): JComponent
    fun createSettingsButton(): JComponent
}
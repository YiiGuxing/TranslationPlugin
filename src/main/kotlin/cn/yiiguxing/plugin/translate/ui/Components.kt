package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.ui.addKeyboardAction
import javax.swing.JComponent
import javax.swing.KeyStroke


/**
 * Registers keyboard actions for transferring focus to the next component.
 *
 * @param keyStrokes The keystrokes that trigger the focus transfer action.
 */
fun JComponent.registerTransferFocusKeyboards(vararg keyStrokes: KeyStroke) {
    addKeyboardAction(*keyStrokes) { transferFocus() }
}

/**
 * Registers keyboard actions for transferring focus backward to the previous component.
 *
 * @param keyStrokes The keystrokes that trigger the focus transfer backward action.
 */
fun JComponent.registerTransferFocusBackwardKeyboards(vararg keyStrokes: KeyStroke) {
    addKeyboardAction(*keyStrokes) { transferFocusBackward() }
}

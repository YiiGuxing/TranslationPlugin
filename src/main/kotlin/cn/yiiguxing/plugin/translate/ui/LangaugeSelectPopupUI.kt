package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.addKeyboardAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import icons.TranslationIcons
import java.awt.Component
import java.awt.Container
import java.awt.DefaultFocusTraversalPolicy
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingConstants

class LangaugeSelectPopupUI(
    presentation: Presentation,
    private val callback: (source: Lang, target: Lang) -> Unit
) : Disposable {

    val component: JComponent = JPanel(HorizontalLayout(3, SwingConstants.CENTER))
    internal val sourceLangComboBox = ComboBox<Lang>()
    internal val targetLangComboBox = ComboBox<Lang>()
    internal val actionButton = ActionButton(
        object : AnAction() {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun actionPerformed(e: AnActionEvent) {
                callback(
                    sourceLangComboBox.selectedItem as Lang,
                    targetLangComboBox.selectedItem as Lang
                )
                popup.closeOk(null)
            }
        },
        presentation,
        ActionPlaces.UNKNOWN,
        ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    )

    private lateinit var popup: JBPopup

    init {
        initComboBoxes()
        initFocusTraversalPolicy()
        initKeyboardActions()
        layout()
    }

    private fun initComboBoxes() {
        sourceLangComboBox.model = LanguageListModel.sorted(Lang.values().toList(), Lang.AUTO)
        targetLangComboBox.model = LanguageListModel.sorted(Lang.values().toList(), Lang.default)

        val customizer: (JBLabel, Lang, Int) -> Unit = { label, value, _ -> label.text = value.localeName }
        sourceLangComboBox.renderer = SimpleListCellRenderer.create(customizer)
        targetLangComboBox.renderer = SimpleListCellRenderer.create(customizer)
    }

    private fun initFocusTraversalPolicy() {
        actionButton.isFocusable = true
        component.isFocusCycleRoot = true
        component.focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
            override fun getDefaultComponent(aContainer: Container): Component = sourceLangComboBox
            override fun getFirstComponent(aContainer: Container): Component = sourceLangComboBox
            override fun getLastComponent(aContainer: Container): Component = actionButton

            override fun getComponentAfter(aContainer: Container, aComponent: Component): Component {
                return when (aComponent) {
                    sourceLangComboBox -> targetLangComboBox
                    targetLangComboBox -> actionButton
                    else -> super.getComponentAfter(aContainer, aComponent)
                }
            }

            override fun getComponentBefore(aContainer: Container, aComponent: Component): Component {
                return when (aComponent) {
                    actionButton -> targetLangComboBox
                    targetLangComboBox -> sourceLangComboBox
                    else -> super.getComponentBefore(aContainer, aComponent)
                }
            }
        }
    }

    private fun initKeyboardActions() {
        val enterKeyStroke = KeyStroke.getKeyStroke("ENTER")
        sourceLangComboBox.registerComboBoxKeyboard(enterKeyStroke)
        targetLangComboBox.registerComboBoxKeyboard(enterKeyStroke)
        actionButton.addKeyboardAction(enterKeyStroke) { actionButton.click() }
        val rightKeyStroke = KeyStroke.getKeyStroke("RIGHT")
        sourceLangComboBox.registerTransferFocusKeyboards(rightKeyStroke)
        targetLangComboBox.registerTransferFocusKeyboards(rightKeyStroke)
        val leftKeyStroke = KeyStroke.getKeyStroke("LEFT")
        targetLangComboBox.registerTransferFocusBackwardKeyboards(leftKeyStroke)
        actionButton.registerTransferFocusBackwardKeyboards(leftKeyStroke)
    }

    private fun ComboBox<*>.registerComboBoxKeyboard(keyStroke: KeyStroke) {
        addKeyboardAction(keyStroke) {
            if (!isPopupVisible) {
                showPopup()
            } else {
                transferFocus()
            }
        }
    }

    private fun layout() {
        component.border = JBUI.Borders.empty(3, 3, 3, 6)
        component.add(sourceLangComboBox)
        component.add(JBLabel().apply { icon = TranslationIcons.Transform })
        component.add(targetLangComboBox)
        component.add(actionButton)
    }

    internal fun setPopup(popup: JBPopup) {
        Disposer.register(popup, this)
        this.popup = popup
        PopupDragHelper.dragPopupByComponent(popup, component)
    }

    fun setSourceLanguages(languages: Collection<Lang>, selection: Lang? = null) {
        sourceLangComboBox.model = LanguageListModel.sorted(languages, selection)
    }

    fun setTargetLanguages(languages: Collection<Lang>, selection: Lang? = null) {
        targetLangComboBox.model = LanguageListModel.sorted(languages, selection)
    }

    override fun dispose() {
    }
}
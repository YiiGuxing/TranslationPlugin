package cn.yiiguxing.plugin.translate.ui.form

import cn.yiiguxing.plugin.translate.TTSSource
import cn.yiiguxing.plugin.translate.TargetLanguageSelection
import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.ui.ActionLink
import cn.yiiguxing.plugin.translate.ui.UI.emptyBorder
import cn.yiiguxing.plugin.translate.ui.UI.fill
import cn.yiiguxing.plugin.translate.ui.UI.fillX
import cn.yiiguxing.plugin.translate.ui.UI.fillY
import cn.yiiguxing.plugin.translate.ui.UI.migLayout
import cn.yiiguxing.plugin.translate.ui.UI.migLayoutVertical
import cn.yiiguxing.plugin.translate.ui.UI.plus
import cn.yiiguxing.plugin.translate.ui.UI.wrap
import cn.yiiguxing.plugin.translate.ui.selected
import cn.yiiguxing.plugin.translate.ui.settings.TranslationEngine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.*
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import icons.Icons
import net.miginfocom.layout.CC
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

abstract class SettingsForm {
    protected val wholePanel: JPanel = JPanel()

    protected val useTranslateGoogleComCheckBox: JBCheckBox =
        JBCheckBox(message("settings.google.options.useGoogleCom"))
    protected val configureTranslationEngineLink: ActionLink = ActionLink(message("settings.configure.link")) {}

    protected val translationEngineComboBox: ComboBox<TranslationEngine> = comboBox<TranslationEngine>().apply {
        renderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value.translatorName
            label.icon = value.icon
        }
        addItemListener {
            fixEngineConfigurationComponent()
        }
    }


    protected val primaryLanguageComboBox: ComboBox<Lang> = comboBox<Lang>().apply {
        renderer = SimpleListCellRenderer.create { label, lang, _ ->
            label.text = lang.langName
        }
    }

    protected val targetLangSelectionComboBox: ComboBox<TargetLanguageSelection> =
        comboBox<TargetLanguageSelection>().apply {
            renderer = SimpleListCellRenderer.create("") { it.displayName }
        }

    protected val takeWordCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.take.word.when.translation.dialog.opens"))
    protected val takeNearestWordCheckBox: JCheckBox = JCheckBox(message("settings.options.take.single.word"))
    protected val keepFormatCheckBox: JBCheckBox = JBCheckBox(message("settings.options.keepFormatting"))

    protected lateinit var ignoreRegExp: EditorTextField
    protected val checkIgnoreRegExpButton: JButton = JButton(message("settings.button.check"))
    protected val ignoreRegExpMsg: JLabel = JLabel()

    protected val separatorsTextField: JTextField = JTextField().apply {
        document = object : PlainDocument() {
            override fun insertString(offset: Int, str: String?, attr: AttributeSet?) {
                val text = getText(0, length)
                val stringToInsert = str
                    ?.filter { it in ' '..'~' && !Character.isLetterOrDigit(it) && !text.contains(it) }
                    ?.toSet()
                    ?.take(10 - length)
                    ?.joinToString("")
                    ?: return
                if (stringToInsert.isNotEmpty()) {
                    super.insertString(offset, stringToInsert, attr)
                }
            }
        }
    }

    protected var primaryFontComboBox: FontComboBox = createFontComboBox(filterNonLatin = false)
    protected var phoneticFontComboBox: FontComboBox = createFontComboBox(filterNonLatin = true)

    protected val primaryFontPreview: JTextComponent = JEditorPane(
        "text/plain",
        message("settings.font.default.preview.text")
    )
    protected val phoneticFontPreview: JLabel = JLabel(PHONETIC_CHARACTERS)
    protected val restoreDefaultButton = JButton(message("settings.button.restore.default.fonts"))

    protected val foldOriginalCheckBox: JBCheckBox = JBCheckBox(message("settings.options.foldOriginal"))

    protected val ttsSourceComboBox: ComboBox<TTSSource> =
        ComboBox(CollectionComboBoxModel(TTSSource.values().asList())).apply {
            renderer = SimpleListCellRenderer.create("") { it.displayName }
            preferredSize = Dimension(preferredSize.width, JBUI.scale(26))
        }

    protected val autoPlayTTSCheckBox: JBCheckBox = JBCheckBox(message("settings.options.autoPlayTTS")).apply {
        addItemListener {
            ttsSourceComboBox.isEnabled = isSelected
        }
    }

    protected val showWordFormsCheckBox: JBCheckBox = JBCheckBox(message("settings.options.showWordForms"))
    protected val autoReplaceCheckBox: JBCheckBox = JBCheckBox(message("settings.options.autoReplace"))
    protected val selectTargetLanguageCheckBox: JBCheckBox = JBCheckBox(message("settings.options.selectLanguage"))
    protected val showWordsOnStartupCheckBox: JBCheckBox = JBCheckBox(message("settings.options.showWordsOnStartup"))
    protected val showExplanationCheckBox: JBCheckBox = JBCheckBox(message("settings.options.showExplanation"))

    protected val maxHistoriesSizeComboBox: ComboBox<Int> = comboBox(50, 30, 20, 10, 0).apply {
        isEditable = true
    }

    protected val clearHistoriesButton: JButton = JButton(message("settings.clear.history.button"))

    protected val cacheSizeLabel: JLabel = JLabel("0KB")
    protected val clearCacheButton: JButton = JButton(message("settings.cache.button.clear"))

    protected val translateDocumentationCheckBox: JBCheckBox =
        JBCheckBox(message("settings.options.translate.documentation"))

    protected val showActionsInContextMenuOnlyWithSelectionCheckbox: JBCheckBox =
        JBCheckBox(message("settings.options.show.actions.only.with.selection"))

    protected val supportLinkLabel: LinkLabel<*> = LinkLabel<Any>(message("support.or.donate"), Icons.Support).apply {
        border = JBUI.Borders.empty(20, 0, 0, 0)
    }

    protected fun doLayout() {
        val generalPanel = titledPanel(message("settings.panel.title.general")) {
            val comboboxGroup = "combobox"

            add(JLabel(message("settings.label.translation.engine")))
            add(translationEngineComboBox, CC().sizeGroupX(comboboxGroup))
            val configurePanel = Box.createHorizontalBox().apply {
                add(useTranslateGoogleComCheckBox)
                add(configureTranslationEngineLink)
                fixEngineConfigurationComponent()
            }
            add(configurePanel, wrap().gapBefore("indent").span(2))

            add(JLabel(message("settings.label.primaryLanguage")))
            add(primaryLanguageComboBox, wrap().sizeGroupX(comboboxGroup))

            add(JLabel(message("settings.label.targetLanguage")))
            add(targetLangSelectionComboBox, wrap().sizeGroupX(comboboxGroup))
        }
        val textSelectionPanel = titledPanel(message("settings.panel.title.text.selection")) {
            add(keepFormatCheckBox, wrap().span(4))
            add(takeNearestWordCheckBox, wrap().span(4))
            add(takeWordCheckBox, wrap().span(4))

            add(JLabel(message("settings.label.ignore")))

            val ignoreRegexComponent: JComponent =
                if (ApplicationManager.getApplication() != null) ignoreRegExp
                else JTextField()

            add(ignoreRegexComponent)
            add(checkIgnoreRegExpButton)
            add(ignoreRegExpMsg, wrap())

            setMinWidth(ignoreRegexComponent)
        }

        val fontsPanel = titledPanel(message("settings.panel.title.fonts")) {
            add(JLabel(message("settings.font.label.primary")))
            add(JLabel(message("settings.font.label.phonetic")), wrap())
            add(primaryFontComboBox)
            add(phoneticFontComboBox, wrap())

            val primaryPreviewPanel = JPanel(migLayout()).apply {
                add(primaryFontPreview, fillX())
            }
            val phoneticPreviewPanel = JPanel(migLayout()).apply {
                add(phoneticFontPreview, fillX().dockNorth())
            }
            add(primaryPreviewPanel, fillX())
            add(phoneticPreviewPanel, fill().wrap())

            add(restoreDefaultButton)

            //compensate custom border of ComboBox
            primaryFontPreview.border = emptyBorder(3) + JBUI.Borders.customLine(JBColor.border())
            primaryPreviewPanel.border = emptyBorder(3)
            phoneticFontPreview.border = emptyBorder(6)
        }

        val translationPopupPanel = titledPanel(message("settings.panel.title.translation.popup")) {
            add(foldOriginalCheckBox, wrap().span(2))
            add(showWordFormsCheckBox, wrap().span(2))
            add(autoPlayTTSCheckBox)
            add(ttsSourceComboBox, wrap())
        }

        val translateAndReplacePanel = titledPanel(message("settings.panel.title.translate.and.replace")) {
            add(selectTargetLanguageCheckBox, wrap().span(2))
            add(autoReplaceCheckBox, wrap().span(2))
            add(JLabel(message("settings.label.separators")).apply {
                toolTipText = message("settings.tip.separators")
            })
            add(separatorsTextField, wrap())
            setMinWidth(separatorsTextField)
        }

        val wordOfTheDayPanel = titledPanel(message("settings.panel.title.word.of.the.day")) {
            add(showWordsOnStartupCheckBox, wrap())
            add(showExplanationCheckBox, wrap())
        }

        val cacheAndHistoryPanel = titledPanel(message("settings.panel.title.cacheAndHistory")) {
            add(JPanel(migLayout()).apply {
                add(JLabel(message("settings.cache.label.diskCache")))
                add(cacheSizeLabel)
                add(clearCacheButton, wrap())
            }, wrap().span(2))
            add(JPanel(migLayout()).apply {
                add(JLabel(message("settings.history.label.maxLength")))
                add(maxHistoriesSizeComboBox)
                add(clearHistoriesButton, wrap())
            }, CC().span(2))
            setMinWidth(maxHistoriesSizeComboBox)

            cacheSizeLabel.border = JBUI.Borders.empty(0, 2, 0, 10)
        }

        val otherPanel = titledPanel(message("settings.panel.title.other")) {
            add(translateDocumentationCheckBox, wrap())
            add(showActionsInContextMenuOnlyWithSelectionCheckbox, wrap())
        }

        wholePanel.addVertically(
            generalPanel,
            fontsPanel,
            textSelectionPanel,
            translationPopupPanel,
            translateAndReplacePanel,
            wordOfTheDayPanel,
            cacheAndHistoryPanel,
            otherPanel,
            supportLinkLabel
        )
    }

    fun createMainPanel(): JPanel {
        doLayout()
        return wholePanel
    }

    private fun fixEngineConfigurationComponent() {
        when (translationEngineComboBox.selected) {
            TranslationEngine.GOOGLE -> {
                useTranslateGoogleComCheckBox.isVisible = true
                configureTranslationEngineLink.isVisible = false
            }
            else -> {
                useTranslateGoogleComCheckBox.isVisible = false
                configureTranslationEngineLink.isVisible = true
            }
        }

    }

    companion object {
        private const val PHONETIC_CHARACTERS = "ˈ'ˌːiɜɑɔuɪeæʌɒʊəaɛpbtdkgfvszθðʃʒrzmnŋhljw"

        private const val MIN_ELEMENT_WIDTH = 80

        private fun setMinWidth(component: JComponent) = component.apply {
            minimumSize = Dimension(MIN_ELEMENT_WIDTH, height)
        }

        private fun createFontComboBox(filterNonLatin: Boolean): FontComboBox =
            FontComboBox(false, filterNonLatin, false)

        private fun titledPanel(title: String, body: JPanel.() -> Unit): JComponent {
            val innerPanel = JPanel(migLayout())
            innerPanel.body()
            return JPanel(migLayout()).apply {
                border = IdeBorderFactory.createTitledBorder(title)
                add(innerPanel)
                add(JPanel(), fillX())
            }
        }

        private fun JPanel.addVertically(vararg components: JComponent) {
            layout = migLayoutVertical()
            components.forEach {
                add(it, fillX())
            }
            add(JPanel(), fillY())
        }

        private fun <T> comboBox(elements: List<T>): ComboBox<T> = ComboBox(CollectionComboBoxModel(elements))

        private fun <T> comboBox(vararg elements: T): ComboBox<T> = comboBox(elements.toList())

        private inline fun <reified T : Enum<T>> comboBox(): ComboBox<T> = comboBox(enumValues<T>().toList())
    }
}
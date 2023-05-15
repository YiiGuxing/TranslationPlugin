package cn.yiiguxing.plugin.translate.ui.wordbook

import cn.yiiguxing.plugin.translate.message
import cn.yiiguxing.plugin.translate.ui.TTSButton
import cn.yiiguxing.plugin.translate.ui.UI
import cn.yiiguxing.plugin.translate.ui.UI.getFonts
import cn.yiiguxing.plugin.translate.ui.Viewer
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.EditorTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

interface WordDetailsUI : Disposable {

    val contentComponent: JComponent

    val wordView: Viewer

    val languageLabel: JBLabel

    val ttsButton: TTSButton

    val phoneticField: EditorTextField

    val tagsField: TextFieldWithAutoCompletion<String>

    val explanationLabel: JBLabel

    val explanationView: JBTextArea


    class Impl internal constructor(project: Project, provider: TagsCompletionProvider) : WordDetailsUI {

        override val contentComponent: JPanel = JPanel(UI.migLayout())

        override val wordView: Viewer = Viewer()
        override val languageLabel: JBLabel = JBLabel()
        override val ttsButton: TTSButton = TTSButton()
        override val phoneticField: EditorTextField = EditorTextField("", project, FileTypes.PLAIN_TEXT)
        override val tagsField: TextFieldWithAutoCompletion<String> =
            TextFieldWithAutoCompletion(project, provider, false, null)
        override val explanationLabel: JBLabel = JBLabel()
        override val explanationView: JBTextArea = JBTextArea()

        init {
            val root = contentComponent
            val gap = JBUIScale.scale(8)
            val (primaryFont, phoneticFont) = getFonts(15, 14)

            JBDimension(500, 450).let { size ->
                root.minimumSize = size
                root.preferredSize = size
            }
            root.border = JBUI.Borders.empty(16)

            wordView.apply {
                border = JBUI.Borders.emptyBottom(16)
                font = primaryFont.biggerOn(5f).asBold()
            }
            root.add(wordView, UI.fillX().wrap())

            val languagePanel = JPanel(HorizontalLayout(JBUIScale.scale(4))).apply {
                add(languageLabel)
                add(ttsButton)
            }
            root.add(languagePanel, UI.fillX().wrap())

            phoneticField.font = phoneticFont
            tagsField.font = phoneticFont
            val phoneticAndTagPanel = JPanel(UI.migLayout("$gap!", insets = "$gap 0 $gap 0")).apply {
                border = JBUI.Borders.customLine(UI.getBorderColor(), 1, 0, 0, 0)
                add(JBLabel(message("word.details.phonetic")))
                add(phoneticField, UI.fillX().wrap())
                add(JBLabel(message("word.details.tags")))
                add(tagsField, UI.fillX().wrap())
            }
            root.add(phoneticAndTagPanel, UI.fillX().gapTop(gap.toString()).wrap())

            explanationView.apply {
                font = primaryFont
                margin = JBUI.insets(6)
            }
            val scrollPane = JBScrollPane(explanationView).apply {
                border = JBUI.Borders.customLine(UI.getBorderColor())
            }
            root.add(explanationLabel, UI.fillX().gapBottom((gap / 2).toString()).wrap())
            root.add(scrollPane, UI.fill().wrap())
        }

        override fun dispose() {
            Disposer.dispose(ttsButton)
        }
    }
}
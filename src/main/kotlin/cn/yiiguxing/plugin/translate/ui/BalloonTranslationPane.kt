package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JScrollPane

class BalloonTranslationPane(
    project: Project?,
    settings: Settings,
    private val maxWidth: Int
) : TranslationPane<LangComboBoxLink>(project, settings) {

    private lateinit var dictViewerScrollWrapper: JScrollPane
    private var lastScrollValue: Int = 0

    private var ignoreEvent = false
    private var onLanguageChangedHandler: ((Lang, Lang) -> Unit)? = null

    override val originalFoldingLength: Int = 100

    val sourceLanguage: Lang? get() = sourceLangComponent.selected
    val targetLanguage: Lang? get() = targetLangComponent.selected

    init {
        border = JBUI.Borders.empty(OFFSET, OFFSET, OFFSET, -GAP)
        maximumSize = Dimension(maxWidth, Int.MAX_VALUE)
        onFixLanguage { sourceLangComponent.selected = it }
    }

    private fun LangComboBoxLink.swap(old: Lang?, new: Lang?) {
        if (new == selected && old != Lang.AUTO && new != Lang.AUTO) {
            ignoreEvent = true
            selected = old
            ignoreEvent = false
        }
    }

    override fun onCreateLanguageComponent(): LangComboBoxLink = LangComboBoxLink().apply {
        isOpaque = false

        addItemListener { newLang, oldLang, _ ->
            if (!ignoreEvent) {
                when (this) {
                    sourceLangComponent -> targetLangComponent.swap(oldLang, newLang)
                    targetLangComponent -> sourceLangComponent.swap(oldLang, newLang)
                }

                val src = sourceLangComponent.selected
                val target = targetLangComponent.selected
                if (src != null && target != null) {
                    onLanguageChangedHandler?.invoke(src, target)
                }
            }
        }
    }

    override fun onWrapViewer(viewer: Viewer): JComponent {
        val maxHeight = if (isOriginalOrTranslationViewer(viewer)) MAX_VIEWER_SMALL else MAX_VIEWER_HEIGHT
        val scrollPane = object : ScrollPane(viewer) {
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                if (preferredSize.height > maxHeight) {
                    return Dimension(preferredSize.width, maxHeight)
                }
                return preferredSize
            }
        }

        viewer.border = JBUI.Borders.emptyRight(OFFSET + GAP)

        if (isDictViewer(viewer)) {
            dictViewerScrollWrapper = scrollPane
        }

        return scrollPane
    }

    override fun onRowCreated(row: JComponent) {
        if (row !is ScrollPane) {
            val border = row.border
            val toMerge = JBUI.Borders.emptyRight(OFFSET + GAP)
            row.border = if (border != null) JBUI.Borders.merge(border, toMerge, false) else toMerge
        }
    }

    override fun onBeforeFoldingExpand() {
        lastScrollValue = dictViewerScrollWrapper.verticalScrollBar.value
        dictViewerScrollWrapper.let {
            it.preferredSize = Dimension(it.width, it.height)
        }
    }

    override fun onFoldingExpanded() {
        dictViewerScrollWrapper.verticalScrollBar.run {
            invokeLater { value = lastScrollValue }
        }
    }

    override fun getPreferredSize(): Dimension {
        val preferredSize = super.getPreferredSize()
        if (preferredSize.width > maxWidth) {
            return Dimension(maxWidth, preferredSize.height)
        }
        return preferredSize
    }

    override fun LangComboBoxLink.updateLanguage(lang: Lang?) {
        ignoreEvent = true
        selected = lang
        ignoreEvent = false
    }

    fun setSupportedLanguages(src: List<Lang>, target: List<Lang>) {
        sourceLangComponent.setLanguages(src)
        targetLangComponent.setLanguages(target)
    }

    fun onLanguageChanged(handler: (src: Lang, target: Lang) -> Unit) {
        onLanguageChangedHandler = handler
    }

    companion object {
        private const val OFFSET = 10

        const val MAX_VIEWER_SMALL = 200
        const val MAX_VIEWER_HEIGHT = 250

        private fun LangComboBoxLink.setLanguages(languages: List<Lang>) {
            model = LanguageListModel.sorted(languages, selected)
        }
    }

}
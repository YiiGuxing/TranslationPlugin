package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.util.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.event.ItemEvent
import javax.swing.JComponent
import javax.swing.JScrollPane

class BalloonTranslationPane(
    project: Project?,
    settings: Settings,
    private val maxWidth: Int
) : TranslationPane<ComboBox<Lang>>(project, settings) {

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

    private fun ComboBox<Lang>.swap(old: Any?, new: Any?) {
        if (new == selectedItem && old != Lang.AUTO && new != Lang.AUTO) {
            ignoreEvent = true
            selectedItem = old
            ignoreEvent = false
        }
    }

    override fun onCreateLanguageComponent(): ComboBox<Lang> = ComboBox<Lang>().apply {
        isOpaque = false
        ui = LangComboBoxUI(this)

        var old: Any? = null
        addItemListener {
            when (it.stateChange) {
                ItemEvent.DESELECTED -> old = it.item
                ItemEvent.SELECTED -> {
                    if (!ignoreEvent) {
                        when (it.source) {
                            sourceLangComponent -> targetLangComponent.swap(old, it.item)
                            targetLangComponent -> sourceLangComponent.swap(old, it.item)
                        }

                        val src = sourceLangComponent.selected
                        val target = targetLangComponent.selected
                        if (src != null && target != null) {
                            onLanguageChangedHandler?.invoke(src, target)
                        }
                    }
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

        viewer.border = JBUI.Borders.empty(0, 0, 0, OFFSET + GAP)

        if (isDictViewer(viewer)) {
            dictViewerScrollWrapper = scrollPane
        }

        return scrollPane
    }

    override fun onRowCreated(row: JComponent) {
        if (row !is ScrollPane) {
            val border = row.border
            val toMerge = JBUI.Borders.empty(0, 0, 0, OFFSET + GAP)
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

    override fun ComboBox<Lang>.updateLanguage(lang: Lang?) {
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

        private fun ComboBox<Lang>.setLanguages(languages: List<Lang>) {
            model = LanguageListModel(languages, selected)
        }
    }

}
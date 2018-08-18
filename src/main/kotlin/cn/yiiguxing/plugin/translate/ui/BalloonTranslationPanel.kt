package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.JBDimension
import java.awt.event.ItemEvent
import javax.swing.JComponent

/**
 * BalloonTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/13
 */
class BalloonTranslationPanel(project: Project?, settings: Settings)
    : TranslationPanel<ComboBox<Lang>>(project, settings) {

    private var ignoreEvent = false
    private var onLanguageChangedHandler: ((Lang, Lang) -> Unit)? = null

    override val originalFoldingLength: Int = 100

    val sourceLanguage: Lang? get() = sourceLangComponent.selected
    val targetLanguage: Lang? get() = targetLangComponent.selected

    init {
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

    override fun onWrapViewer(viewer: Viewer): JComponent = ScrollPane(viewer).apply {
        val maxHeight = if (viewer === originalViewer || viewer === transViewer) {
            MAX_VIEWER_SMALL
        } else {
            MAX_VIEWER_HEIGHT
        }
        maximumSize = JBDimension(MAX_WIDTH, maxHeight)
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
        const val MAX_VIEWER_SMALL = 200
        const val MAX_VIEWER_HEIGHT = 250

        private fun ComboBox<Lang>.setLanguages(languages: List<Lang>) {
            model = LanguageListModel(languages, selected)
        }
    }

}
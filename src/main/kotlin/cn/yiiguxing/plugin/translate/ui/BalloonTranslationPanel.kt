package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.trans.Lang
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBEmptyBorder
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.JComponent

/**
 * BalloonTranslationPanel
 *
 * Created by Yii.Guxing on 2017/12/13
 */
class BalloonTranslationPanel(settings: Settings) : TranslationPanel<ComboBox<Lang>>(settings) {

    private var ignoreEvent = false
    private var onLanguageChangedHandler: ((Lang, Lang) -> Unit)? = null
    private val itemListener = ItemListener {
        with(it) {
            if (!ignoreEvent && stateChange == ItemEvent.SELECTED) {
                val src = sourceLangComponent.selected
                val target = targetLangComponent.selected
                if (src != null && target != null) {
                    onLanguageChangedHandler?.invoke(src, target)
                }
            }
        }
    }

    override fun onCreateLanguageComponent(): ComboBox<Lang> = ComboBox<Lang>().apply {
        ui = LangComboBoxUI(this)
        addItemListener(itemListener)
    }

    override fun onWrapViewer(viewer: Viewer): JComponent = JBScrollPane(viewer).apply {
        isOpaque = false
        border = JBEmptyBorder(0)
        verticalScrollBar = createVerticalScrollBar()
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

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
        sourceLangComponent.apply {
            setLanguages(src)
            updateLanguage(srcLang)
        }
        targetLangComponent.setLanguages(target)
    }

    fun onLanguageChanged(handler: (src: Lang, target: Lang) -> Unit) {
        onLanguageChangedHandler = handler
    }

    companion object {
        const val MAX_VIEWER_SMALL = 200
        const val MAX_VIEWER_HEIGHT = 300

        private fun ComboBox<Lang>.setLanguages(languages: List<Lang>) {
            model = CollectionComboBoxModel<Lang>(languages, selected)
        }
    }

}
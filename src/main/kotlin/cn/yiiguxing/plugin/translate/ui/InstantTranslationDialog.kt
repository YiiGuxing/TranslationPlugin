package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.View
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.form.InstantTranslationDialogForm
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import java.awt.datatransfer.StringSelection
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent

/**
 * InstantTranslationDialog
 *
 * Created by Yii.Guxing on 2018/06/18
 */
class InstantTranslationDialog(project: Project?) : InstantTranslationDialogForm(project), View, Disposable {

    private var _disposed = false
    override val disposed get() = _disposed

    private val focusManager: IdeFocusManager = IdeFocusManager.getInstance(project)

    init {
        title = "Translation"
        isModal = false
        initComponents()
        peer.setContentPane(createCenterPanel())
    }

    private fun initComponents() {
        inputScrollPane.border = null
        translationScrollPane.border = null
        inputContentPanel.border = BORDER
        translationContentPanel.border = BORDER
        inputToolBar.apply {
            border = TOOLBAR_BORDER
            background = TOOLBAR_BACKGROUND
        }
        translationToolBar.apply {
            border = TOOLBAR_BORDER
            background = TOOLBAR_BACKGROUND
        }

        inputTTSButton.isEnabled = false
        translationTTSButton.isEnabled = false
        clearButton.apply {
            isEnabled = false
            icon = Icons.ClearText
            disabledIcon = Icons.ClearTextDisabled
            setHoveringIcon(Icons.ClearTextHovering)
            setListener({ _, _ -> inputTextArea.text = "" }, null)
        }
        copyButton.apply {
            isEnabled = false
            icon = Icons.CopyAll
            disabledIcon = Icons.CopyAllDisabled
            setHoveringIcon(Icons.CopyAllHovering)
            setListener({ _, _ ->
                val textToCopy = translationTextArea
                        .selectedText
                        .takeUnless { it.isNullOrEmpty() }
                        ?: translationTextArea.text
                if (!textToCopy.isNullOrEmpty()) {
                    CopyPasteManager.getInstance().setContents(StringSelection(textToCopy))
                }
            }, null)
        }

        inputTextArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val isNotEmpty = e.document.length > 0
                clearButton.isEnabled = isNotEmpty
                inputTTSButton.isEnabled = isNotEmpty
            }
        })
        translationTextArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val isNotEmpty = e.document.length > 0
                copyButton.isEnabled = isNotEmpty
                translationTTSButton.isEnabled = isNotEmpty
            }
        })
    }

    override fun showStartTranslate(text: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showTranslation(translation: Translation) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showError(errorMessage: String, throwable: Throwable) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun show() {
        if (!isShowing) {
            super.show()
        } else {
            focusManager.requestFocus(window, true)
        }
    }

    fun close() {
        close(CLOSE_EXIT_CODE)
    }

    override fun dispose() {
        if (disposed) {
            return
        }

        super.dispose()
        _disposed = true

        Disposer.dispose(this)
        println("Instant translate dialog disposed.")
    }

    companion object {
        private val BORDER = LineBorder(JBColor(0x808080, 0x303030))
        private val TOOLBAR_BORDER = SideBorder(JBColor(0x9F9F9F, 0x3C3C3C), SideBorder.TOP)
        private val TOOLBAR_BACKGROUND = JBColor(0xEEF1F3, 0x4E5556)
    }
}
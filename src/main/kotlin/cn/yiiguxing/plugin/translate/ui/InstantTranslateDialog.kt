package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.ui.form.InstantTranslateDialogForm
import cn.yiiguxing.plugin.translate.ui.icon.Icons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import javax.swing.border.LineBorder

/**
 * InstantTranslateDialog
 *
 * Created by Yii.Guxing on 2018/06/18
 */
class InstantTranslateDialog(project: Project?) : InstantTranslateDialogForm(project) {

    init {
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

        clearButton.apply {
            icon = Icons.ClearText
            disabledIcon = Icons.ClearTextDisabled
            setHoveringIcon(Icons.ClearTextHovering)
        }
        copyButton.apply {
            icon = Icons.CopyAll
            disabledIcon = Icons.CopyAllDisabled
            setHoveringIcon(Icons.CopyAllHovering)
        }
    }

    companion object {
        private val BORDER = LineBorder(JBColor(0x808080, 0x303030))
        private val TOOLBAR_BORDER = SideBorder(JBColor(0x9F9F9F, 0x3C3C3C), SideBorder.TOP)
        private val TOOLBAR_BACKGROUND = JBColor(0xEEF1F3, 0x4E5556)
    }
}
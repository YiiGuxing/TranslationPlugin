package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.trans.Lang;
import cn.yiiguxing.plugin.translate.ui.TTSButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InstantTranslationDialogForm extends DialogWrapper {
    private JPanel mRoot;
    protected ComboBox<Lang> sourceLangComboBox;
    protected ComboBox<Lang> targetLangComboBox;
    protected JButton swapButton;
    protected JButton translateButton;
    protected JTextArea inputTextArea;
    protected JTextArea translationTextArea;
    protected TTSButton inputTTSButton;
    protected TTSButton translationTTSButton;
    protected LinkLabel<Void> clearButton;
    protected LinkLabel<Void> copyButton;
    protected JScrollPane inputScrollPane;
    protected JScrollPane translationScrollPane;
    protected JPanel translationToolBar;
    protected JPanel inputToolBar;
    protected JPanel inputContentPanel;
    protected JPanel translationContentPanel;

    protected InstantTranslationDialogForm(@Nullable Project project) {
        super(project);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mRoot;
    }
}

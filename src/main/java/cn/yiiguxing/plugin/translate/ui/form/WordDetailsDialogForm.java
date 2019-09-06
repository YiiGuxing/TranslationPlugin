package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Word details dialog form.
 * <p>
 * Created by Yii.Guxing on 2019/09/06.
 */
public class WordDetailsDialogForm extends DialogWrapper {
    protected JPanel rootPanel;
    protected Viewer wordView;
    protected JLabel languageLabel;
    protected TTSButton ttsButton;
    protected JBTextField phoneticField;
    protected JLabel explanationLabel;
    protected JScrollPane scrollPane;
    protected Viewer explanationView;

    protected WordDetailsDialogForm() {
        super(true);
    }

    private void createUIComponents() {
        wordView = new Viewer();
        explanationView = new Viewer();
        ttsButton = new TTSButton();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }
}

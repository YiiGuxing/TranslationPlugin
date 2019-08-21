package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * WordOfDayDialogForm
 * <p>
 * Created by Yii.Guxing on 2019/08/19.
 */
public class WordOfDayDialogForm extends DialogWrapper {

    private JPanel rootPanel;
    protected Viewer wordView;
    protected JLabel phoneticLabel;
    protected TTSButton ttsButton;
    protected Viewer explainsView;
    protected JButton showExplainsButton;

    protected WordOfDayDialogForm(@Nullable Project project) {
        super(project);
    }

    private void createUIComponents() {
        wordView = new Viewer();
        explainsView = new Viewer();
        ttsButton = new TTSButton();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return rootPanel;
    }

}

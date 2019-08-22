package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.Gray;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * WordOfDayDialogForm
 * <p>
 * Created by Yii.Guxing on 2019/08/19.
 */
public class WordDialogForm extends DialogWrapper {

    private JPanel rootPanel;
    protected Viewer wordView;
    protected JLabel phoneticLabel;
    protected TTSButton ttsButton;
    protected Viewer explainsView;
    protected JButton showExplainsButton;

    protected WordDialogForm(@Nullable Project project) {
        super(project);

        if (SystemInfo.isWin10OrNewer && !UIUtil.isUnderDarcula()) {
            rootPanel.setBorder(JBUI.Borders.customLine(Gray.xD0, 1, 0, 0, 0));
        }
        rootPanel.setBorder(JBUI.Borders.empty(8, 12));
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

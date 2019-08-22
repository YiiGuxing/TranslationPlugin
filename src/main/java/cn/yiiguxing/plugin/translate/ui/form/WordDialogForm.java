package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Word of the day dialog form
 * <p>
 * Created by Yii.Guxing on 2019/08/19.
 */
public class WordDialogForm extends DialogWrapper {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 200;
    private static final Color BACKGROUND_COLOR = new JBColor(0xFFFFFF, 0x45494A);
    private static final Color BACKGROUND_COLOR_EXPLAINS = new JBColor(0xF0F0F0, 0x535758);

    private JPanel rootPanel;
    protected Viewer wordView;
    protected JLabel phoneticLabel;
    protected TTSButton ttsButton;
    protected JPanel explainsCard;
    protected Viewer explainsView;
    protected JButton showExplainsButton;

    protected WordDialogForm(@Nullable Project project) {
        super(project);

        rootPanel.setBackground(BACKGROUND_COLOR);
        rootPanel.setBorder(JBUI.Borders.empty(10, 12));
        rootPanel.setPreferredSize(new JBDimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        wordView.setBorder(null);
        explainsCard.setBackground(BACKGROUND_COLOR_EXPLAINS);
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

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
    private static final int DEFAULT_HEIGHT = 160;
    private static final Color BORDER_COLOR = new JBColor(0xE0E0E0, 0x5D6162);
    private static final Color BACKGROUND_COLOR = new JBColor(0xFFFFFF, 0x494D4E);
    private static final Color BACKGROUND_COLOR_EXPLAINS = new JBColor(0xF2F2F2, 0x535758);
    private static final Color SCROLL_BAR_BACKGROUND_COLOR_EXPLAINS = new JBColor(0xF0F0F0, 0x515556);

    private JPanel rootPanel;
    private JScrollPane scrollPane;
    protected Viewer wordView;
    protected TTSButton ttsButton;
    protected JPanel explainsCard;
    protected JPanel maskPanel;
    protected Viewer explainsView;
    protected JButton showExplanationButton;
    protected JLabel explanationLabel;

    protected WordDialogForm(@Nullable Project project) {
        super(project);

        rootPanel.setBackground(BACKGROUND_COLOR);
        rootPanel.setBorder(JBUI.Borders.empty(10, 12));
        rootPanel.setPreferredSize(new JBDimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        wordView.setBorder(null);
        scrollPane.setBorder(JBUI.Borders.customLine(BORDER_COLOR));
        scrollPane.setBackground(BACKGROUND_COLOR_EXPLAINS);
        explainsCard.setBackground(BACKGROUND_COLOR_EXPLAINS);
        scrollPane.getHorizontalScrollBar().setBackground(SCROLL_BAR_BACKGROUND_COLOR_EXPLAINS);
        scrollPane.getVerticalScrollBar().setBackground(SCROLL_BAR_BACKGROUND_COLOR_EXPLAINS);
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

package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * Word details dialog form.
 * <p>
 * Created by Yii.Guxing on 2019/09/06.
 */
public class WordDetailsDialogForm extends DialogWrapper {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 200;

    protected JPanel contentPanel;
    private JPanel actionsPanel;
    protected Viewer wordView;
    protected JLabel languageLabel;
    protected TTSButton ttsButton;
    protected EditorTextField phoneticField;
    protected JLabel explanationLabel;
    protected JScrollPane scrollPane;
    protected JEditorPane explanationView;
    protected JButton saveEditingButton;
    protected JButton cancelEditingButton;
    protected JButton closeButton;
    protected JPanel tagsPanel;

    protected WordDetailsDialogForm(Project project) {
        super(project);

        wordView.setBorder(null);
        WordFormUtil.setRootPanelStyle(contentPanel, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        WordFormUtil.setExplanationPaneBorder(explanationView);
        WordFormUtil.setFonts(wordView, phoneticField, explanationView);

        Color background = UIManager.getColor("TextArea.background");
        actionsPanel.setBackground(background);
        explanationView.setBackground(background);
        scrollPane.setBackground(background);
        scrollPane.getVerticalScrollBar().setBackground(background);
        scrollPane.getHorizontalScrollBar().setBackground(background);

        final Color color = UIManager.getColor("DialogWrapper.southPanelDivider");
        final Border line = new CustomLineBorder(color != null ? color : OnePixelDivider.BACKGROUND, 1, 0, 0, 0);
        actionsPanel.setBorder(new CompoundBorder(line, JBUI.Borders.empty(5, 10)));

        getRootPane().setDefaultButton(closeButton);
    }

    private void createUIComponents() {
        wordView = new Viewer();
        ttsButton = new TTSButton();
    }

    @NotNull
    @Override
    protected DialogStyle getStyle() {
        return DialogStyle.COMPACT;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }

    @Override
    protected JComponent createSouthPanel() {
        return actionsPanel;
    }
}

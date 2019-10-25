package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.TTSButton;
import cn.yiiguxing.plugin.translate.ui.UI;
import cn.yiiguxing.plugin.translate.ui.Viewer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Word details dialog form.
 */
public class WordDetailsDialogForm extends DialogWrapper {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 300;

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
    private JPanel neckPanel;

    protected WordDetailsDialogForm(Project project) {
        super(project);

        wordView.setBorder(JBUI.Borders.empty(0, 2, 10, 2));
        WordFormUtil.setRootPanelStyle(contentPanel, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        WordFormUtil.setExplanationPaneBorder(explanationView);
        WordFormUtil.setFonts(wordView, phoneticField, explanationView);

        neckPanel.setBorder(JBUI.Borders.empty(10, 0));
        ((GridLayoutManager) neckPanel.getLayout()).setVGap(JBUI.scale(5));

        Color background = UI.getColor("TextArea.background");
        explanationView.setBackground(background);
        scrollPane.setBackground(background);
        scrollPane.getVerticalScrollBar().setBackground(background);
        scrollPane.getHorizontalScrollBar().setBackground(background);

        Color bordersColor = UI.getBordersColor(JBColor.GRAY);
        scrollPane.setBorder(new LineBorder(bordersColor));

        final Border line = new CustomLineBorder(bordersColor, 1, 0, 0, 0);
        actionsPanel.setBorder(new CompoundBorder(line, JBUI.Borders.empty(5, 10)));
        actionsPanel.setBackground(UI.getColor("ToolWindow.Header.background", new JBColor(0xDCDCDC, 0x343739)));

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

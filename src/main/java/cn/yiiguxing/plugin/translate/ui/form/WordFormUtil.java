package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.ui.UI;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;

import javax.swing.*;

/**
 * WordFormUtil
 */
final class WordFormUtil {

    private WordFormUtil() {
    }

    static void setRootPanelStyle(JPanel rootPanel, int width, int height) {
        rootPanel.setBorder(JBUI.Borders.empty(10, 12));
        rootPanel.setPreferredSize(new JBDimension(width, height));
        rootPanel.setMinimumSize(new JBDimension(width, height));
    }

    static void setExplanationPaneBorder(JComponent pane) {
        pane.setBorder(JBUI.Borders.empty(10));
    }

    static void setFonts(JComponent word, JComponent phonetic, JComponent explanation) {
        UI.FontPair fonts = UI.getFonts(15, 14);
        word.setFont(fonts.getPrimary().biggerOn(3f).asBold());
        phonetic.setFont(fonts.getPhonetic());
        explanation.setFont(fonts.getPrimary());
    }

}

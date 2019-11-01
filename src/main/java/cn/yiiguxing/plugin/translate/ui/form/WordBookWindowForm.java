package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.wordbook.WordBookItem;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.table.TableView;

import javax.swing.*;

/**
 * Word book window form.
 */
public class WordBookWindowForm extends SimpleToolWindowPanel {
    protected JPanel root;
    protected LinkLabel<?> downloadLinkLabel;
    protected TableView<WordBookItem> tableView;

    public WordBookWindowForm() {
        super(true, true);
        setContent(root);
    }

    private void createUIComponents() {
        downloadLinkLabel = new LinkLabel("", AllIcons.General.Warning);
    }
}

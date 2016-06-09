package cn.yiiguxing.plugin.translate;


import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

public class TranslationComponent {

    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 450;

    private static final JBColor MSG_FOREGROUND = new JBColor(new Color(0xFF333333), new Color(0xFFBBBBBB));
    private static final JBColor MSG_FOREGROUND_ERROR = new JBColor(new Color(0xFF333333), new Color(0xFFEE0000));

    private static final int MAX_HISTORIES_SIZE = 20;
    private static final DefaultComboBoxModel<String> COMBO_BOX_MODEL = new DefaultComboBoxModel<>();

    private JPanel contentPane;
    private JButton queryBtn;
    private JLabel messageLabel;
    private JPanel msgPanel;
    private JTextPane resultText;
    private JScrollPane scrollPane;
    private JComboBox<String> queryComboBox;

    private String currentQuery;

    private JBPopup translationPopup;

    TranslationComponent() {
        initViews();
        translationPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(contentPane, null)
                .setTitle("Translation")
                .setMinSize(new Dimension(MIN_WIDTH, MIN_HEIGHT))
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnWindowDeactivation(true)
                .setResizable(true)
                .setMovable(true)
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setKeyEventHandler(keyEvent -> {
                    if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                        query(queryComboBox.getEditor().getItem().toString());
                        return true;
                    }

                    return false;
                })
                .createPopup();
    }

    void showInBestPositionFor(AnActionEvent e) {
        Editor editor = EDITOR.getData(e.getDataContext());
        if (editor != null) {
            translationPopup.showInBestPositionFor(editor);

            String selectedText = editor.getSelectionModel().getSelectedText();
            query(selectedText);
        } else if (e.getProject() != null) {
            translationPopup.showCenteredInCurrentWindow(e.getProject());
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Point point = new Point((screenSize.width - MIN_WIDTH) / 2, (screenSize.height - MIN_HEIGHT) / 2);
            translationPopup.show(new RelativePoint(point));
        }

        queryComboBox.requestFocus();
    }

    private void initViews() {
        queryBtn.setEnabled(false);
        queryBtn.addActionListener(e -> query(queryComboBox.getEditor().getItem().toString()));

        initQueryComboBox();

        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());

        JBColor background = new JBColor(new Color(0xFFFFFFFF), new Color(0xFF2B2B2B));
        messageLabel.setBackground(background);
        msgPanel.setBackground(background);
        resultText.setBackground(background);
        scrollPane.setBackground(background);

        setComponentPopupMenu();
    }

    @SuppressWarnings("unchecked")
    private void initQueryComboBox() {
        queryComboBox.setModel(COMBO_BOX_MODEL);

        JTextField field = (JTextField) queryComboBox.getEditor().getEditorComponent();
        field.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                updateQueryButton();
            }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setSelectionStart(0);
                field.setSelectionEnd(field.getText().length());
            }
        });

        queryComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateQueryButton();
                onQuery();
            }
        });
        queryComboBox.setRenderer(new ListCellRendererWrapper<String>() {

            @Override
            public void customize(JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value != null && value.trim().length() > 20) {
                    String trim = value.trim();
                    setText(trim.substring(0, 15) + "..." + trim.substring(trim.length() - 3));
                } else {
                    setText(value == null ? "" : value.trim());
                }
            }
        });
    }

    private void setComponentPopupMenu() {
        JBPopupMenu menu = new JBPopupMenu();

        JBMenuItem copy = new JBMenuItem("Copy", IconLoader.getIcon("/actions/copy_dark.png"));
        copy.addActionListener(e -> {
            String selectedText = resultText.getSelectedText();
            if (!Utils.isEmptyString(selectedText)) {
                CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
                copyPasteManager.setContents(new StringSelection(selectedText));
            }
        });


        JBMenuItem query = new JBMenuItem("Query", IconLoader.getIcon("/icon_16.png"));
        query.addActionListener(e -> {
            String selectedText = resultText.getSelectedText();
            if (!Utils.isEmptyString(selectedText)) {
                query(selectedText);
            }
        });

        menu.add(copy);
        menu.add(query);

        menu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelectedText = !Utils.isEmptyString(resultText.getSelectedText());
                copy.setEnabled(hasSelectedText);
                query.setEnabled(hasSelectedText);
            }
        });

        resultText.setComponentPopupMenu(menu);
    }

    void query(String query) {
        if (Utils.isEmptyString(query)) {
            if (COMBO_BOX_MODEL.getSize() > 0) {
                updateQueryButton();
            }

            return;
        }

        resultText.setText("");

        queryComboBox.getEditor().setItem(query);
        COMBO_BOX_MODEL.removeElement(query);
        COMBO_BOX_MODEL.insertElementAt(query, 0);
        queryComboBox.setSelectedIndex(0);

        /*
         * queryComboBox失焦时纠正误触发onQuery后，正常调用query却是不会触发ItemEvent了，需要手动调用onQuery().
         */
        if (query.equals(queryComboBox.getSelectedItem())) {
            onQuery();
        }

        if (COMBO_BOX_MODEL.getSize() > MAX_HISTORIES_SIZE) {
            COMBO_BOX_MODEL.removeElementAt(MAX_HISTORIES_SIZE);
        }
    }

    private void updateQueryButton() {
        queryBtn.setEnabled(!Utils.isEmptyString(queryComboBox.getEditor().getItem().toString()));
    }

    private void onQuery() {
        /*
         * queryComboBox失焦时会误触发
         */
        String text = queryComboBox.getSelectedItem().toString();
        if (COMBO_BOX_MODEL.getIndexOf(text) < 0)
            return;

        currentQuery = text;
        if (!Utils.isEmptyString(currentQuery)) {
            currentQuery = currentQuery.trim();
            messageLabel.setForeground(MSG_FOREGROUND);
            messageLabel.setText("Querying...");
            msgPanel.setVisible(true);
            scrollPane.setVisible(false);
            Translation.get().search(currentQuery, new QueryCallback(this));
        }
    }

    private void onPostResult(String query, QueryResult result) {
        if (Utils.isEmptyString(currentQuery) || !currentQuery.equals(query))
            return;

        String errorMessage = Utils.getErrorMessage(result);
        if (errorMessage != null) {
            messageLabel.setText(errorMessage);
            messageLabel.setForeground(MSG_FOREGROUND_ERROR);
            return;
        }

        Document document = resultText.getDocument();
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return;
        }

        Utils.insertHeader(document, result);

        BasicExplain basicExplain = result.getBasicExplain();
        if (basicExplain != null) {
            Utils.insertExplain(document, basicExplain.getExplains());
        } else {
            Utils.insertExplain(document, result.getTranslation());
        }

        WebExplain[] webExplains = result.getWebExplains();
        Utils.insertWebExplain(document, webExplains);

        resultText.setCaretPosition(0);
        scrollPane.setVisible(true);
        msgPanel.setVisible(false);
    }

    private static class QueryCallback implements Translation.Callback {
        private final Reference<TranslationComponent> dialogReference;

        private QueryCallback(TranslationComponent dialog) {
            this.dialogReference = new WeakReference<>(dialog);
        }

        @Override
        public void onQuery(String query, QueryResult result) {
            TranslationComponent dialog = dialogReference.get();
            if (dialog != null) {
                dialog.onPostResult(query, result);
            }
        }
    }

}

package cn.yiiguxing.plugin.translate;


import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class TranslateDialog extends JDialog {

    private static final JBColor MSG_FOREGROUND = new JBColor(new Color(0xFF333333), new Color(0xFFBBBBBB));
    private static final JBColor MSG_FOREGROUND_ERROR = new JBColor(new Color(0xFF333333), new Color(0xFFEE0000));

    private JPanel contentPane;
    private JTextField queryText;
    private JButton queryBtn;
    private JLabel messageLabel;
    private JPanel msgPanel;
    private JTextPane resultText;
    private JScrollPane scrollPane;

    private String currentQuery;

    public TranslateDialog() {
        setTitle("Translate");
        setMinimumSize(new Dimension(400, 450));
        setModal(true);
        setLocationRelativeTo(null);
        setContentPane(contentPane);

        initViews();
    }

    private void initViews() {
        queryBtn.addActionListener(e -> onQuery());
        queryText.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                queryBtn.setEnabled(!Utils.isEmptyString(queryText.getText()));
            }
        });

        getRootPane().setDefaultButton(queryBtn);
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());

        JBColor background = new JBColor(new Color(0xFFFFFFFF), new Color(0xFF2B2B2B));
        messageLabel.setBackground(background);
        msgPanel.setBackground(background);
        resultText.setBackground(background);
        scrollPane.setBackground(background);
    }

    public void query(String query) {
        if (Utils.isEmptyString(query)) {
            query = "";
        }
        queryText.setText(query);
        resultText.setText("");
        onQuery();
    }

    private void onQuery() {
        currentQuery = queryText.getText();
        if (!Utils.isEmptyString(currentQuery)) {
            currentQuery = currentQuery.trim();
            messageLabel.setForeground(MSG_FOREGROUND);
            messageLabel.setText("Querying...");
            msgPanel.setVisible(true);
            scrollPane.setVisible(false);
            Translate.get().search(currentQuery, new QueryCallback(this));
        }
    }

    void onPostResult(String query, QueryResult result) {
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

    private static class QueryCallback implements Translate.Callback {
        private final Reference<TranslateDialog> dialogReference;

        private QueryCallback(TranslateDialog dialog) {
            this.dialogReference = new WeakReference<>(dialog);
        }

        @Override
        public void onQuery(String query, QueryResult result) {
            TranslateDialog dialog = dialogReference.get();
            if (dialog != null) {
                dialog.onPostResult(query, result);
            }
        }
    }

}

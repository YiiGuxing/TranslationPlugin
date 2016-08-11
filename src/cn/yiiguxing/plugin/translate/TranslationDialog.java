package cn.yiiguxing.plugin.translate;


import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;

public class TranslationDialog extends JDialog implements TranslationView {

    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 450;

    private static final JBColor MSG_FOREGROUND = new JBColor(new Color(0xFF333333), new Color(0xFFBBBBBB));
    private static final JBColor MSG_FOREGROUND_ERROR = new JBColor(new Color(0xFF333333), new Color(0xFFEE0000));

    private JPanel titlePanel;
    private JPanel contentPane;
    private JButton queryBtn;
    private JLabel messageLabel;
    private JPanel msgPanel;
    private JTextPane resultText;
    private JScrollPane scrollPane;
    private JComboBox<String> queryComboBox;

    private final MyModel mModel;
    private final TranslationPresenter mTranslationPresenter;

    private String mLastQuery;
    private boolean mBroadcast;

    TranslationDialog() {
        setUndecorated(true);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setModal(false);
        setLocationRelativeTo(null);
        setContentPane(contentPane);

        mTranslationPresenter = new TranslationPresenter(this);
        mModel = new MyModel(mTranslationPresenter.getHistory());

        initViews();

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                if (isShowing()) {
                    setVisible(false);
                }
            }
        });
    }

    private void createUIComponents() {
        TitlePanel panel = new TitlePanel();
        panel.setText("Translation");
        panel.setActive(true);

        WindowMoveListener window = new WindowMoveListener(panel);
        panel.addMouseListener(window);
        panel.addMouseMotionListener(window);

        titlePanel = panel;
        titlePanel.requestFocus();
    }

    private void initViews() {
        JRootPane rootPane = this.getRootPane();

        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        queryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onQueryButtonClick();
            }
        });
        rootPane.setDefaultButton(queryBtn);

        initQueryComboBox();

        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());

        JBColor background = new JBColor(new Color(0xFFFFFFFF), new Color(0xFF2B2B2B));
        messageLabel.setBackground(background);
        msgPanel.setBackground(background);
        resultText.setBackground(background);
        scrollPane.setBackground(background);

        setComponentPopupMenu();
    }

    private void onQueryButtonClick() {
        String query = resultText.getSelectedText();
        if (Utils.isEmptyOrBlankString(query)) {
            query = queryComboBox.getEditor().getItem().toString();
        }
        query(query);
    }

    private void initQueryComboBox() {
        queryComboBox.setModel(mModel);

        final JTextField field = (JTextField) queryComboBox.getEditor().getEditorComponent();
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setSelectionStart(0);
                field.setSelectionEnd(field.getText().length());
            }
        });

        queryComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && !mBroadcast) {
                    onQuery();
                }
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

        final JBMenuItem copy = new JBMenuItem("Copy", IconLoader.getIcon("/actions/copy_dark.png"));
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedText = resultText.getSelectedText();
                if (!Utils.isEmptyOrBlankString(selectedText)) {
                    CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
                    copyPasteManager.setContents(new StringSelection(selectedText));
                }
            }
        });

        final JBMenuItem query = new JBMenuItem("Query", IconLoader.getIcon("/icon_16.png"));
        query.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                query(resultText.getSelectedText());
            }
        });

        menu.add(copy);
        menu.add(query);

        menu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelectedText = !Utils.isEmptyOrBlankString(resultText.getSelectedText());
                copy.setEnabled(hasSelectedText);
                query.setEnabled(hasSelectedText);
            }
        });

        resultText.setComponentPopupMenu(menu);
    }

    void show(Editor editor) {
        String query = null;
        if (editor != null) {
            query = Utils.splitWord(editor.getSelectionModel().getSelectedText());
        }
        if (Utils.isEmptyOrBlankString(query) && mModel.getSize() > 0) {
            query = mModel.getElementAt(0);
        }

        query(query);
        setVisible(true);
    }

    private void query(String query) {
        if (!Utils.isEmptyOrBlankString(query)) {
            queryComboBox.getEditor().setItem(query);
            onQuery();
        }
    }

    private void onQuery() {
        String text = queryComboBox.getEditor().getItem().toString();
        if (!Utils.isEmptyOrBlankString(text) && !text.equals(mLastQuery)) {
            resultText.setText("");
            messageLabel.setForeground(MSG_FOREGROUND);
            messageLabel.setText("Querying...");
            msgPanel.setVisible(true);
            scrollPane.setVisible(false);
            mTranslationPresenter.query(text);
        }
    }

    @Override
    public void updateHistory() {
        mBroadcast = true;// 防止递归查询
        mModel.fireContentsChanged();
        queryComboBox.setSelectedIndex(0);
        mBroadcast = false;
    }

    @Override
    public void showResult(@NotNull String query, @NotNull QueryResult result) {
        mLastQuery = query;
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

    @Override
    public void showError(@NotNull String error) {
        messageLabel.setText(error);
        messageLabel.setForeground(MSG_FOREGROUND_ERROR);
    }

    private static class MyModel extends AbstractListModel<String> implements ComboBoxModel<String> {
        private final List<String> myFullList;
        private Object mySelectedItem;

        MyModel(@NotNull List<String> list) {
            myFullList = list;
        }

        @Override
        public String getElementAt(int index) {
            return this.myFullList.get(index);
        }

        @Override
        public int getSize() {
            return myFullList.size();
        }

        @Override
        public Object getSelectedItem() {
            return this.mySelectedItem;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            this.mySelectedItem = anItem;
            this.fireContentsChanged();
        }

        void fireContentsChanged() {
            this.fireContentsChanged(this, -1, -1);
        }

    }

}

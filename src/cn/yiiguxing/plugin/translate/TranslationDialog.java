package cn.yiiguxing.plugin.translate;


import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.AnimatedIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.List;

public class TranslationDialog extends DialogWrapper implements TranslationView {

    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 450;

    private static final JBColor MSG_FOREGROUND_ERROR = new JBColor(new Color(0xFF333333), new Color(0xFFFF2222));

    private static final Border BORDER_ACTIVE = new LineBorder(new JBColor(JBColor.GRAY, Gray._35));
    private static final Border BORDER_PASSIVE = new LineBorder(new JBColor(JBColor.LIGHT_GRAY, Gray._75));

    private JPanel titlePanel;
    private JPanel contentPane;
    private JButton queryBtn;
    private JLabel messageLabel;
    private JPanel msgPanel;
    private JTextPane resultText;
    private JScrollPane scrollPane;
    private JComboBox<String> queryComboBox;
    private JPanel textPanel;
    private JPanel processPanel;
    private AnimatedIcon processIcon;
    private CardLayout layout;

    private final MyModel mModel;
    private final TranslationPresenter mTranslationPresenter;

    private String mLastQuery;
    private boolean mBroadcast;

    private boolean mLastMoveWasInsideDialog;
    private final AWTEventListener mAwtActivityListener = new AWTEventListener() {

        @Override
        public void eventDispatched(AWTEvent e) {
            if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_MOVED) {
                final boolean inside = isInside(new RelativePoint((MouseEvent) e));
                if (inside != mLastMoveWasInsideDialog) {
                    mLastMoveWasInsideDialog = inside;
                    ((MyTitlePanel) titlePanel).myButton.repaint();
                }
            }
        }
    };

    TranslationDialog(@Nullable Project project) {
        super(project);
        setUndecorated(true);
        setModal(false);
        getPeer().setContentPane(createCenterPanel());

        mTranslationPresenter = new TranslationPresenter(this);
        mModel = new MyModel(mTranslationPresenter.getHistory());

        initViews();

        getRootPane().setOpaque(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(mAwtActivityListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(mAwtActivityListener);
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        contentPane.setPreferredSize(JBUI.size(MIN_WIDTH, MIN_HEIGHT));
        contentPane.setBorder(BORDER_ACTIVE);

        return contentPane;
    }

    private void createUIComponents() {
        final MyTitlePanel panel = new MyTitlePanel();
        panel.setText("Translation");
        panel.setActive(true);

        WindowMoveListener windowListener = new WindowMoveListener(panel);
        panel.addMouseListener(windowListener);
        panel.addMouseMotionListener(windowListener);

        getWindow().addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                panel.setActive(true);
                contentPane.setBorder(BORDER_ACTIVE);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                panel.setActive(false);
                contentPane.setBorder(BORDER_PASSIVE);
            }
        });

        titlePanel = panel;
        titlePanel.requestFocus();

        processIcon = new ProcessIcon();
    }

    private boolean isInside(@NotNull RelativePoint target) {
        Component cmp = target.getOriginalComponent();

        if (!cmp.isShowing()) return true;
        if (cmp instanceof MenuElement) return false;
        Window window = this.getWindow();
        if (UIUtil.isDescendingFrom(cmp, window)) return true;
        if (!isShowing()) return false;
        Point point = target.getScreenPoint();
        SwingUtilities.convertPointFromScreen(point, window);
        return window.contains(point);
    }

    private void initViews() {
        queryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onQueryButtonClick();
            }
        });
        getRootPane().setDefaultButton(queryBtn);

        initQueryComboBox();

        textPanel.setBorder(BORDER_ACTIVE);
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());

        JBColor background = new JBColor(new Color(0xFFFFFFFF), new Color(0xFF2B2B2B));
        messageLabel.setBackground(background);
        processPanel.setBackground(background);
        msgPanel.setBackground(background);
        resultText.setBackground(background);
        scrollPane.setBackground(background);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        layout = (CardLayout) textPanel.getLayout();
        layout.show(textPanel, "msg");

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

    public void show() {
        if (!isShowing()) {
            super.show();
        }

        if (mModel.getSize() > 0) {
            query(mModel.getElementAt(0));
        }
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
            processIcon.resume();
            layout.show(textPanel, "process");
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

        Utils.insertQueryResultText(resultText.getDocument(), result);

        resultText.setCaretPosition(0);
        layout.show(textPanel, "result");
        processIcon.suspend();
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

    private static final Icon CLOSE_ICON = IconLoader.getIcon("/close.png");
    private static final Icon CLOSE_PRESSED = IconLoader.getIcon("/closePressed.png");

    private class MyTitlePanel extends TitlePanel {

        final CloseButton myButton;

        MyTitlePanel() {
            super();

            myButton = new CloseButton();
            add(myButton, BorderLayout.EAST);

            NonOpaquePanel panel = new NonOpaquePanel();
            panel.setPreferredSize(myButton.getPreferredSize());
            add(panel, BorderLayout.WEST);

            setActive(false);
        }

        @Override
        public void setActive(boolean active) {
            super.setActive(active);
            if (myButton != null) {
                myButton.setActive(active);
            }
        }
    }

    private class CloseButton extends NonOpaquePanel {

        private boolean isPressedByMouse;
        private boolean isActive;

        CloseButton() {
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    TranslationDialog.this.dispose();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressedByMouse = true;
                    CloseButton.this.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressedByMouse = false;
                    CloseButton.this.repaint();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isPressedByMouse = false;
                    CloseButton.this.repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(CLOSE_ICON.getIconWidth() + JBUI.scale(4), CLOSE_ICON.getIconHeight());
        }

        private void setActive(final boolean active) {
            this.isActive = active;
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (hasPaint()) {
                paintIcon(g, !isActive || isPressedByMouse ? CLOSE_PRESSED : CLOSE_ICON);
            }
        }

        private boolean hasPaint() {
            return getWidth() > 0 && mLastMoveWasInsideDialog;
        }

        private void paintIcon(@NotNull Graphics g, @NotNull Icon icon) {
            icon.paintIcon(this, g, JBUI.scale(2), (getHeight() - icon.getIconHeight()) / 2);
        }
    }
}

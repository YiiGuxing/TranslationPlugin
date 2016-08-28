package cn.yiiguxing.plugin.translate.ui;


import cn.yiiguxing.plugin.translate.TranslationContract;
import cn.yiiguxing.plugin.translate.TranslationPresenter;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.model.QueryResult;
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

public class TranslationDialog extends DialogWrapper implements TranslationContract.View {

    private static final int MIN_WIDTH = 400;
    private static final int MIN_HEIGHT = 450;

    private static final JBColor MSG_FOREGROUND_ERROR = new JBColor(new Color(0xFFFF2222), new Color(0xFFFF2222));

    private static final Border BORDER_ACTIVE = new LineBorder(new JBColor(JBColor.GRAY, Gray._35));
    private static final Border BORDER_PASSIVE = new LineBorder(new JBColor(JBColor.LIGHT_GRAY, Gray._75));

    private static final String CARD_MSG = "msg";
    private static final String CARD_PROCESS = "process";
    private static final String CARD_RESULT = "result";

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
    private JLabel queryingLabel;
    private CardLayout layout;

    private final MyModel mModel;
    private final TranslationContract.Presenter mTranslationPresenter;

    private String mLastQuery;
    private boolean mBroadcast;

    private OnDisposeListener mOnDisposeListener;

    private boolean mLastMoveWasInsideDialog;
    private final AWTEventListener mAwtActivityListener = new AWTEventListener() {

        @Override
        public void eventDispatched(AWTEvent e) {
            final int id = e.getID();
            if (e instanceof MouseEvent && id == MouseEvent.MOUSE_MOVED) {
                final boolean inside = isInside(new RelativePoint((MouseEvent) e));
                if (inside != mLastMoveWasInsideDialog) {
                    mLastMoveWasInsideDialog = inside;
                    ((MyTitlePanel) titlePanel).myButton.repaint();
                }
            }

            if (e instanceof KeyEvent && id == KeyEvent.KEY_PRESSED) {
                final KeyEvent ke = (KeyEvent) e;
                // Close the dialog if ESC is pressed
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    close(CLOSE_EXIT_CODE);
                }
            }
        }
    };

    public TranslationDialog(@Nullable Project project) {
        super(project);
        setUndecorated(true);
        setModal(false);
        getPeer().setContentPane(createCenterPanel());

        mTranslationPresenter = new TranslationPresenter(this);
        mModel = new MyModel(mTranslationPresenter.getHistory());

        initViews();

        getRootPane().setOpaque(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(mAwtActivityListener, AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.KEY_EVENT_MASK);
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
        getWindow().addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                queryComboBox.requestFocus();
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
        layout.show(textPanel, CARD_MSG);

        queryingLabel.setForeground(new JBColor(new Color(0xFF4C4C4C), new Color(0xFFCDCDCD)));

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
                field.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.select(0, 0);
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

    public void setOnDisposeListener(OnDisposeListener listener) {
        mOnDisposeListener = listener;
    }

    @Override
    protected void dispose() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(mAwtActivityListener);

        if (mOnDisposeListener != null) {
            mOnDisposeListener.onDispose();
        }

        super.dispose();
    }

    public void show() {
        if (!isShowing()) {
            super.show();
        }

        update();
        getWindow().requestFocus();
    }

    public void update() {
        if (isShowing()) {
            if (mModel.getSize() > 0) {
                query(mModel.getElementAt(0));
            }
        }
    }

    public void query(String query) {
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
            layout.show(textPanel, CARD_PROCESS);
            mTranslationPresenter.query(text);
        }
    }

    public void updateHistory(boolean updateComboBox) {
        mModel.fireContentsChanged();

        mBroadcast = true;// 防止递归查询
        if (updateComboBox) {
            queryComboBox.setSelectedIndex(0);
        } else if (mLastQuery != null) {
            mModel.setSelectedItem(mLastQuery);
        }
        mBroadcast = false;
    }

    @Override
    public void updateHistory() {
        updateHistory(true);
    }

    @Override
    public void showResult(@NotNull String query, @NotNull QueryResult result) {
        mLastQuery = query;

        Utils.insertQueryResultText(resultText.getDocument(), result);

        resultText.setCaretPosition(0);
        layout.show(textPanel, CARD_RESULT);
        processIcon.suspend();
    }

    @Override
    public void showError(@NotNull String error) {
        messageLabel.setText(error);
        messageLabel.setForeground(MSG_FOREGROUND_ERROR);
        layout.show(textPanel, CARD_MSG);
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

            int offset = JBUI.scale(2);
            setBorder(new EmptyBorder(0, myButton.getPreferredSize().width + offset, 0, offset));

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
                    TranslationDialog.this.close(CLOSE_EXIT_CODE);
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
            return new Dimension(CLOSE_ICON.getIconWidth(), CLOSE_ICON.getIconHeight());
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
            icon.paintIcon(this, g, 0, (getHeight() - icon.getIconHeight()) / 2);
        }
    }

    public interface OnDisposeListener {
        void onDispose();
    }

}

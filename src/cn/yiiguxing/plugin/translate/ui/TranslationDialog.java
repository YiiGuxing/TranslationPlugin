package cn.yiiguxing.plugin.translate.ui;


import cn.yiiguxing.plugin.translate.Styles;
import cn.yiiguxing.plugin.translate.TranslationContract;
import cn.yiiguxing.plugin.translate.TranslationPresenter;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.model.BasicExplain;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
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

    private final Project mProject;

    private JPanel mTitlePanel;
    private JPanel mContentPane;
    private JButton mQueryBtn;
    private JLabel mMessageLabel;
    private JPanel mMsgPanel;
    private JTextPane mResultText;
    private JScrollPane mScrollPane;
    @SuppressWarnings("Since15")
    private JComboBox<String> mQueryComboBox;
    private JPanel mTextPanel;
    private JPanel mProcessPanel;
    private AnimatedIcon mProcessIcon;
    private JLabel mQueryingLabel;
    private CardLayout mLayout;

    private final MyModel mModel;
    private final TranslationContract.Presenter mTranslationPresenter;

    private String mLastSuccessfulQuery;
    private boolean mBroadcast;

    private boolean mLastMoveWasInsideDialog;
    private final AWTEventListener mAwtActivityListener = new AWTEventListener() {

        @Override
        public void eventDispatched(AWTEvent e) {
            final int id = e.getID();
            if (e instanceof MouseEvent && id == MouseEvent.MOUSE_MOVED) {
                final boolean inside = isInside(new RelativePoint((MouseEvent) e));
                if (inside != mLastMoveWasInsideDialog) {
                    mLastMoveWasInsideDialog = inside;
                    ((MyTitlePanel) mTitlePanel).myButton.repaint();
                }
            }

            if (e instanceof KeyEvent && id == KeyEvent.KEY_RELEASED) {
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
        this.mProject = project;

        setUndecorated(true);
        setModal(false);
        getPeer().setContentPane(createCenterPanel());

        mTranslationPresenter = new TranslationPresenter(this);
        mModel = new MyModel(mTranslationPresenter.getHistory());

        initViews();

        getRootPane().setOpaque(false);

        Toolkit.getDefaultToolkit().addAWTEventListener(mAwtActivityListener, AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.KEY_EVENT_MASK);

        Disposer.register(getDisposable(), new Disposable() {
            @Override
            public void dispose() {
                Toolkit.getDefaultToolkit().removeAWTEventListener(mAwtActivityListener);
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        mContentPane.setPreferredSize(JBUI.size(MIN_WIDTH, MIN_HEIGHT));
        mContentPane.setBorder(BORDER_ACTIVE);

        return mContentPane;
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
                mContentPane.setBorder(BORDER_ACTIVE);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                panel.setActive(false);
                mContentPane.setBorder(BORDER_PASSIVE);
            }
        });
        getWindow().addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // 放到这里是因为在Android Studio上第一次显示会被queryBtn抢去焦点。
                        mQueryComboBox.requestFocus();
                    }
                });
            }
        });

        mTitlePanel = panel;
        mTitlePanel.requestFocus();

        mProcessIcon = new ProcessIcon();
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
        mQueryBtn.setIcon(Icons.Translate);
        mQueryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onQueryButtonClick();
            }
        });
        getRootPane().setDefaultButton(mQueryBtn);

        initQueryComboBox();

        mTextPanel.setBorder(BORDER_ACTIVE);
        mScrollPane.setVerticalScrollBar(mScrollPane.createVerticalScrollBar());

        JBColor background = new JBColor(new Color(0xFFFFFFFF), new Color(0xFF2B2B2B));
        mMessageLabel.setBackground(background);
        mProcessPanel.setBackground(background);
        mMsgPanel.setBackground(background);
        mResultText.setBackground(background);
        mResultText.setFont(JBUI.Fonts.create("Microsoft YaHei", 14));
        mScrollPane.setBackground(background);
        mScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        mLayout = (CardLayout) mTextPanel.getLayout();
        mLayout.show(mTextPanel, CARD_MSG);

        mQueryingLabel.setForeground(new JBColor(new Color(0xFF4C4C4C), new Color(0xFFCDCDCD)));

        setComponentPopupMenu();
    }

    private void onQueryButtonClick() {
        String query = mResultText.getSelectedText();
        if (Utils.isEmptyOrBlankString(query)) {
            query = mQueryComboBox.getEditor().getItem().toString();
        }
        query(query);
    }

    private void initQueryComboBox() {
        mQueryComboBox.setModel(mModel);

        final JTextField field = (JTextField) mQueryComboBox.getEditor().getEditorComponent();
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

        mQueryComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED && !mBroadcast) {
                    onQuery();
                }
            }
        });
        mQueryComboBox.setRenderer(new ComboRenderer());
    }

    private void setComponentPopupMenu() {
        JBPopupMenu menu = new JBPopupMenu();

        final JBMenuItem copy = new JBMenuItem("Copy", Icons.Copy);
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mResultText.copy();
            }
        });

        final JBMenuItem query = new JBMenuItem("Query", Icons.Translate);
        query.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                query(mResultText.getSelectedText());
            }
        });

        menu.add(copy);
        menu.add(query);

        menu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelectedText = !Utils.isEmptyOrBlankString(mResultText.getSelectedText());
                copy.setEnabled(hasSelectedText);
                query.setEnabled(hasSelectedText);
            }
        });

        mResultText.setComponentPopupMenu(menu);
    }

    public void show() {
        if (!isShowing()) {
            super.show();
        }

        update();
        IdeFocusManager.getInstance(mProject).requestFocus(getContentPane(), true);
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
            mQueryComboBox.getEditor().setItem(query);
            onQuery();
        }
    }

    private void onQuery() {
        String text = mQueryComboBox.getEditor().getItem().toString();
        if (!Utils.isEmptyOrBlankString(text) && !text.equals(mLastSuccessfulQuery)) {
            mResultText.setText("");
            mProcessIcon.resume();
            mLayout.show(mTextPanel, CARD_PROCESS);
            mTranslationPresenter.query(text);
        }
    }

    public void updateHistory(boolean updateComboBox) {
        mModel.fireContentsChanged();

        mBroadcast = true;// 防止递归查询
        if (updateComboBox) {
            mQueryComboBox.setSelectedIndex(0);
        } else if (mLastSuccessfulQuery != null) {
            mModel.setSelectedItem(mLastSuccessfulQuery);
        }
        mBroadcast = false;
    }

    @Override
    public void updateHistory() {
        updateHistory(true);
    }

    @Override
    public void showResult(@NotNull final String query, @NotNull QueryResult result) {
        mLastSuccessfulQuery = query;

        Styles.insertStylishResultText(mResultText, result, new Styles.OnTextClickListener() {
            @Override
            public void onTextClick(@NotNull JTextPane textPane, @NotNull String text) {
                query(text);
            }
        });

        mResultText.setCaretPosition(0);
        mLayout.show(mTextPanel, CARD_RESULT);
        mProcessIcon.suspend();
    }

    @Override
    public void showError(@NotNull String query, @NotNull String error) {
        mLastSuccessfulQuery = null;

        mMessageLabel.setText(error);
        mMessageLabel.setForeground(MSG_FOREGROUND_ERROR);
        mLayout.show(mTextPanel, CARD_MSG);
    }

    @SuppressWarnings("Since15")
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

    private final class ComboRenderer extends ListCellRendererWrapper<String> {
        private final StringBuilder builder = new StringBuilder();
        private final StringBuilder tipBuilder = new StringBuilder();

        @Override
        public void customize(JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            if (list.getWidth() == 0 // 在没有确定大小之前不设置真正的文本,否则控件会被过长的文本撑大.
                    || Utils.isEmptyOrBlankString(value)) {
                setText("");
            } else {
                setRenderText(value);
            }
        }

        private void setRenderText(@NotNull String value) {
            final StringBuilder builder = this.builder;
            final StringBuilder tipBuilder = this.tipBuilder;

            builder.setLength(0);
            tipBuilder.setLength(0);

            builder.append("<html><b>")
                    .append(value)
                    .append("</b>");
            tipBuilder.append(builder);

            final QueryResult cache = mTranslationPresenter.getCache(value);
            if (cache != null) {
                BasicExplain basicExplain = cache.getBasicExplain();
                String[] translation = basicExplain != null ? basicExplain.getExplains() : cache.getTranslation();

                if (translation != null && translation.length > 0) {
                    builder.append("  -  <i><small>");
                    tipBuilder.append("<p/><i>");

                    for (String tran : translation) {
                        builder.append(tran).append("; ");
                        tipBuilder.append(tran).append("<br/>");
                    }

                    builder.setLength(builder.length() - 2);
                    builder.append("</small></i>");

                    tipBuilder.setLength(builder.length() - 5);
                    tipBuilder.append("</i>");
                }
            }

            builder.append("</html>");
            setText(builder.toString());

            tipBuilder.append("</html>");
            setToolTipText(tipBuilder.toString());
        }
    }

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

    private class CloseButton extends IconButton {

        CloseButton() {
            super(Icons.Close, Icons.ClosePressed, new Consumer<MouseEvent>() {
                @Override
                public void consume(MouseEvent mouseEvent) {
                    if (mouseEvent.getClickCount() == 1) {
                        TranslationDialog.this.close(CLOSE_EXIT_CODE);
                    }
                }
            });
        }

        protected boolean hasPaint() {
            return super.hasPaint() && mLastMoveWasInsideDialog;
        }

    }

}

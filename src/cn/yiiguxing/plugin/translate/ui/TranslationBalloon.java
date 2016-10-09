package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.TranslationContract;
import cn.yiiguxing.plugin.translate.TranslationDialogManager;
import cn.yiiguxing.plugin.translate.TranslationPresenter;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonBuilder;
import cn.yiiguxing.plugin.translate.ui.balloon.BalloonImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Consumer;
import com.intellij.util.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class TranslationBalloon implements TranslationContract.View {

    private static final int MIN_BALLOON_WIDTH = JBUI.scale(200);
    private static final int MIN_BALLOON_HEIGHT = JBUI.scale(50);
    private static final int MAX_BALLOON_SIZE = JBUI.scale(600);
    private static final JBInsets BORDER_INSETS = JBUI.insets(20, 20, 20, 20);

    private final JBPanel mContentPanel;
    private final GroupLayout mLayout;
    private JPanel mProcessPanel;
    private AnimatedIcon mProcessIcon;
    private JLabel mQueryingLabel;

    private Balloon mBalloon;
    private RelativePoint mTarget;

    private boolean mInterceptDispose;
    @NotNull
    private final Disposable mDisposable = new Disposable() {
        @Override
        public void dispose() {
            onDispose();
        }
    };

    @NotNull
    private final TranslationContract.Presenter mTranslationPresenter;

    @NotNull
    private final Editor mEditor;
    @Nullable
    private final Project mProject;
    private final RangeMarker mCaretRangeMarker;

    public TranslationBalloon(@NotNull Editor editor, @NotNull RangeMarker caretRangeMarker) {
        mEditor = Utils.requireNonNull(editor, "editor cannot be null");
        mCaretRangeMarker = Utils.requireNonNull(caretRangeMarker, "caretRangeMarker cannot be null");

        updateCaretPosition();

        mContentPanel = new JBPanel<JBPanel>();
        mLayout = new GroupLayout(mContentPanel);
        mContentPanel.setOpaque(false);
        mContentPanel.setLayout(mLayout);

        mQueryingLabel.setForeground(new JBColor(new Color(0xFF4C4C4C), new Color(0xFFCDCDCD)));

        mProcessPanel.setOpaque(false);
        mLayout.setHorizontalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(mProcessPanel, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        mLayout.setVerticalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(mProcessPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        mContentPanel.add(mProcessPanel);
        mProcessIcon.resume();

        mTranslationPresenter = new TranslationPresenter(this);

        mProject = editor.getProject();
        if (mProject != null) {
            Disposer.register(mProject, mDisposable);
        }
    }

    private void createUIComponents() {
        mProcessIcon = new ProcessIcon();
    }

    private void updateCaretPosition() {
        if (mCaretRangeMarker.isValid()) {
            int offset = Math.round((mCaretRangeMarker.getStartOffset() + mCaretRangeMarker.getEndOffset()) / 2f);
            mEditor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, mEditor.offsetToVisualPosition(offset));
        }
    }

    @NotNull
    public Disposable getDisposable() {
        return mDisposable;
    }

    private void onDispose() {
        mBalloon = null;
        mCaretRangeMarker.dispose();
    }

    @NotNull
    private BalloonBuilder buildBalloon() {
        return BalloonBuilder.builder(mContentPanel, null)
                .setHideOnClickOutside(true)
                .setShadow(true)
                .setHideOnKeyOutside(true)
                .setBlockClicksThroughBalloon(true)
                .setBorderInsets(BORDER_INSETS);
    }

    public void showAndQuery(@NotNull String queryText) {
        mBalloon = buildBalloon().setCloseButtonEnabled(false).createBalloon();
        registerDisposer(mBalloon, true);

        mEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        showBalloon(mBalloon);

        mTranslationPresenter.query(Utils.requireNonNull(queryText, "queryText cannot be null"));
    }

    private void registerDisposer(@NotNull Balloon balloon, final boolean intercept) {
        if (mProject != null) {
            Disposer.register(mProject, balloon);
        }
        Disposer.register(balloon, new Disposable() {
            @Override
            public void dispose() {
                if (intercept && mInterceptDispose) {
                    return;
                }

                Disposer.dispose(mDisposable);
                mInterceptDispose = false;
            }
        });
    }

    private void showBalloon(@NotNull final Balloon balloon) {
        final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        balloon.show(new PositionTracker<Balloon>(mEditor.getContentComponent()) {
            @Override
            public RelativePoint recalculateLocation(Balloon object) {
                if (mTarget != null && !popupFactory.isBestPopupLocationVisible(mEditor)) {
                    return mTarget;
                }

                updateCaretPosition();

                final RelativePoint target = popupFactory.guessBestPopupLocation(mEditor);
                Rectangle visibleArea = mEditor.getScrollingModel().getVisibleArea();
                Point point = new Point(visibleArea.x, visibleArea.y);
                SwingUtilities.convertPointToScreen(point, getComponent());

                final Point screenPoint = target.getScreenPoint();
                int y = screenPoint.y - point.y;
                if (mTarget != null && y + balloon.getPreferredSize().getHeight() > visibleArea.height) {
                    //FIXME 只是判断垂直方向，没有判断水平方向，但水平方向问题不是很大。
                    //FIXME 垂直方向上也只是判断Balloon显示在下方的情况，还是有些小问题。
                    return mTarget;
                }

                mTarget = new RelativePoint(new Point(screenPoint.x, screenPoint.y));
                return mTarget;
            }
        }, Balloon.Position.below);
    }

    @Override
    public void updateHistory() {
        TranslationDialogManager.getInstance().notifyHistoriesChanged();
    }

    @Override
    public void showResult(@NotNull String query, @NotNull QueryResult result) {
        if (mBalloon != null) {
            if (mBalloon.isDisposed()) {
                return;
            }

            mInterceptDispose = true;
            mBalloon.hide(true);
        } else {
            return;
        }

        TranslationDialogManager.getInstance().updateCurrentShowingTranslationDialog();

        mContentPanel.remove(0);
        mProcessIcon.suspend();
        mProcessIcon.dispose();

        JTextPane resultText = new JTextPane();
        resultText.setEditable(false);
        resultText.setBackground(UIManager.getColor("Panel.background"));
        resultText.setFont(JBUI.Fonts.create("Microsoft YaHei", 14));

        Utils.insertQueryResultText(resultText, result);
        resultText.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(resultText);
        scrollPane.setBorder(new JBEmptyBorder(0));
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());
        scrollPane.setHorizontalScrollBar(scrollPane.createHorizontalScrollBar());

        mLayout.setHorizontalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, MAX_BALLOON_SIZE));
        mLayout.setVerticalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, MIN_BALLOON_HEIGHT, GroupLayout.DEFAULT_SIZE, MAX_BALLOON_SIZE));
        mContentPanel.add(scrollPane);

        updateCaretPosition();
        final BalloonImpl balloon = (BalloonImpl) buildBalloon().createBalloon();
        RelativePoint showPoint = JBPopupFactory.getInstance().guessBestPopupLocation(mEditor);
        createPinButton(balloon, showPoint);
        registerDisposer(balloon, false);
        showBalloon(balloon);

        // 再刷新一下，尽可能地消除滚动条
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!balloon.isDisposed()) {
                    balloon.revalidate();
                }
            }
        });
    }

    private void createPinButton(final BalloonImpl balloon, final RelativePoint showPoint) {
        // FIXME 由于现在的Balloon是可以移动的，所以showPoint不再那么准确了，可以会使得PinButton显示位置不对。
        balloon.setActionProvider(new BalloonImpl.ActionProvider() {
            private BalloonImpl.ActionButton myPinButton;
            private final Icon myIcon = Icons.Pin;

            @NotNull
            public List<BalloonImpl.ActionButton> createActions() {
                myPinButton = balloon.new ActionButton(myIcon, myIcon, null,
                        new Consumer<MouseEvent>() {
                            @Override
                            public void consume(MouseEvent mouseEvent) {
                                if (mouseEvent.getClickCount() == 1) {
                                    balloon.hide(true);
                                    TranslationDialogManager.getInstance().showTranslationDialog(mEditor.getProject());
                                }
                            }
                        });

                return Collections.singletonList(myPinButton);
            }

            public void layout(@NotNull Rectangle lpBounds) {
                if (myPinButton.isVisible()) {
                    int iconWidth = myIcon.getIconWidth();
                    int iconHeight = myIcon.getIconHeight();
                    int margin = JBUI.scale(3);
                    int x = lpBounds.x + lpBounds.width - iconWidth - margin;
                    int y = lpBounds.y + margin;

                    Rectangle rectangle = new Rectangle(x, y, iconWidth, iconHeight);
                    Insets border = balloon.getShadowBorderInsets();
                    rectangle.x -= border.left;

                    int showX = showPoint.getPoint().x;
                    int showY = showPoint.getPoint().y;
                    // 误差
                    int offset = JBUI.scale(1);
                    boolean atRight = showX <= lpBounds.x + offset;
                    boolean atLeft = showX >= (lpBounds.x + lpBounds.width - offset);
                    boolean below = lpBounds.y >= showY;
                    boolean above = (lpBounds.y + lpBounds.height) <= showY;
                    if (atRight || atLeft || below || above) {
                        rectangle.y += border.top;
                    }

                    myPinButton.setBounds(rectangle);
                }
            }
        });
    }

    @Override
    public void showError(@NotNull String query, @NotNull String error) {
        if (mBalloon == null || mBalloon.isDisposed())
            return;

        mContentPanel.remove(0);
        mProcessIcon.suspend();
        mProcessIcon.dispose();

        JBLabel label = new JBLabel();
        label.setFont(JBUI.Fonts.label(16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setText("Querying...");

        mLayout.setHorizontalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, MIN_BALLOON_WIDTH, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        mLayout.setVerticalGroup(mLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        label.setForeground(new JBColor(new Color(0xFFFF2222), new Color(0xFFFF2222)));
        label.setText(error);
        mContentPanel.add(label);

        mBalloon.revalidate();
    }
}

package cn.yiiguxing.plugin.translate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Consumer;
import com.intellij.util.ui.JBEmptyBorder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class TranslationBalloon implements TranslationView {

    private final JBPanel contentPanel;
    private final GroupLayout layout;
    private final JBLabel label;

    private Balloon balloon;

    private final TranslationPresenter mTranslationPresenter;

    private final Editor editor;

    public TranslationBalloon(@NotNull Editor editor) {
        this.editor = Objects.requireNonNull(editor, "editor cannot be null");

        contentPanel = new JBPanel<>();
        layout = new GroupLayout(contentPanel);
        contentPanel.setLayout(layout);

        label = new JBLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setText("Querying...");

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, JBUI.scale(150), GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(label, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        contentPanel.add(label);

        mTranslationPresenter = new TranslationPresenter(this);
    }

    @NotNull
    private Balloon buildBalloon() {
        return JBPopupFactory.getInstance()
                .createDialogBalloonBuilder(contentPanel, null)
                .setHideOnClickOutside(true)
                .setShadow(true)
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(true)
                .setBorderInsets(JBUI.insets(20, 20, 20, 20))
                .createBalloon();
    }

    public void showAndQuery(@NotNull String queryText) {
        balloon = buildBalloon();
        balloon.show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below);
        mTranslationPresenter.query(Objects.requireNonNull(queryText, "queryText cannot be null"));
    }

    private void createTackButton(final BalloonImpl balloon) {
        balloon.setActionProvider(
                new BalloonImpl.ActionProvider() {
                    private BalloonImpl.ActionButton myCloseButton;

                    @NotNull
                    public List<BalloonImpl.ActionButton> createActions() {
                        this.myCloseButton = balloon.new ActionButton(AllIcons.Ide.Error, AllIcons.Ide.Link, null, new Consumer<MouseEvent>() {
                            @Override
                            public void consume(MouseEvent mouseEvent) {

                            }
                        });
                        return Collections.singletonList(this.myCloseButton);
                    }

                    public void layout(@NotNull Rectangle lpBounds) {
                        if (this.myCloseButton.isVisible()) {
                            Icon icon = balloon.getCloseButton();
                            int iconWidth = icon.getIconWidth();
                            int iconHeight = icon.getIconHeight();
                            Rectangle r = new Rectangle(lpBounds.x + lpBounds.width - iconWidth + (int) ((double) iconWidth * 0.3D), lpBounds.y - (int) ((double) iconHeight * 0.3D), iconWidth, iconHeight);
                            Insets border = balloon.getShadowBorderInsets();
                            r.x -= border.left;
                            r.y -= border.top;
                            this.myCloseButton.setBounds(r);
                        }
                    }
                });
    }

    @Override
    public void updateHistory() {
        // do nothing
    }

    @Override
    public void showResult(@NotNull String query, @NotNull QueryResult result) {
        if (this.balloon != null) {
            if (this.balloon.isDisposed()) {
                return;
            }

            this.balloon.hide(true);
        }

        contentPanel.remove(label);

        JTextPane resultText = new JTextPane();
        resultText.setEditable(false);
        resultText.setFont(JBUI.Fonts.create("Microsoft YaHei", JBUI.scaleFontSize(14)));

        Utils.insertQueryResultText(resultText.getDocument(), result);
        resultText.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(resultText);
        scrollPane.setBorder(new JBEmptyBorder(0));
        scrollPane.setVerticalScrollBar(scrollPane.createVerticalScrollBar());
        scrollPane.setHorizontalScrollBar(scrollPane.createHorizontalScrollBar());

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, JBUI.scale(150), GroupLayout.DEFAULT_SIZE, JBUI.scale(600)));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, JBUI.scale(50), GroupLayout.DEFAULT_SIZE, JBUI.scale(600)));
        contentPanel.add(scrollPane);

        final Balloon balloon = buildBalloon();
        balloon.show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below);

        // 再刷新一下，尽可能地消除滚动条
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                balloon.revalidate();
            }
        });
    }

    @Override
    public void showError(@NotNull String error) {
        if (balloon == null)
            return;

        label.setForeground(new JBColor(new Color(0xFF333333), new Color(0xFFFF2222)));
        label.setText(error);
        balloon.revalidate();
    }
}

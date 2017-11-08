package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.Settings;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;

/**
 * TranslateApiContainer
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
public class TranslateApiContainer extends JPanel implements ConfigurablePanel {

    private JPanel mRootPanel;
    private JPanel mContentPanel;
    private ComboBox<Item> mComboBox;

    private final CardLayout mLayout = new FixedSizeCardLayout();
    private final ArrayList<TranslateApiPanel> mTranslateApiPanels = new ArrayList<>();
    private final Settings mSettings;

    public TranslateApiContainer(@NotNull Settings settings) {
        super(new BorderLayout());
        mSettings = settings;

        init();
    }

    private void init() {
        add(mRootPanel);
        mContentPanel.setLayout(mLayout);

        add(new GoogleApiPanel());
        add(new YoudaoApiPanel());
        mComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                mLayout.show(mContentPanel, ((Item) e.getItem()).id);
            }
        });
    }

    private void add(TranslateApiPanel translateApiPanel) {
        mContentPanel.add(translateApiPanel.getId(), translateApiPanel.getComponent());
        mComboBox.addItem(new Item(translateApiPanel.getId(), translateApiPanel.getTitle()));
        mTranslateApiPanels.add(translateApiPanel);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public boolean isModified() {
        return mTranslateApiPanels.stream().anyMatch(TranslateApiPanel::isModified);
    }

    @Override
    public void reset() {
        mTranslateApiPanels.forEach(TranslateApiPanel::reset);
    }

    @Override
    public void apply() {
        mTranslateApiPanels.forEach(TranslateApiPanel::apply);
    }

    private static class Item {
        final String id;
        final String title;

        Item(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}

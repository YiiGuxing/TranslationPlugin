package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.AppStorage;
import cn.yiiguxing.plugin.translate.Settings;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ListCellRendererWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * TranslateApiContainer
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
@SuppressWarnings("WeakerAccess")
public class TransPanelContainer extends JPanel implements ConfigurablePanel {

    private JPanel mRootPanel;
    private JPanel mContentPanel;
    private ComboBox<TransPanel> mComboBox;

    private final CardLayout mLayout = new FixedSizeCardLayout();
    private final Settings mSettings;
    private final AppStorage mAppStorage;

    public TransPanelContainer(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        super(new BorderLayout());
        mSettings = settings;
        mAppStorage = appStorage;

        init();
    }

    private void init() {
        add(mRootPanel);
        mContentPanel.setLayout(mLayout);

        add(new GoogleTranslatePanel(mSettings.getGoogleTranslateSettings()));
        add(new YoudaoTranslatePanel(mSettings.getYoudaoTranslateSettings()));

        mComboBox.setRenderer(new ListCellRendererWrapper<TransPanel>() {
            @Override
            public void customize(JList list, TransPanel value, int index, boolean selected, boolean hasFocus) {
                setText(value.getName());
                setIcon(value.getIcon());
            }
        });
        mComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                mLayout.show(mContentPanel, ((TransPanel) e.getItem()).getId());
            }
        });
    }

    private void add(TransPanel panel) {
        mContentPanel.add(panel.getId(), panel.getComponent());
        mComboBox.addItem(panel);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public boolean isModified() {
        TransPanel selectedPanel = (TransPanel) mComboBox.getSelectedItem();
        if (selectedPanel == null || !Objects.equals(selectedPanel.getId(), mSettings.getTranslator())) {
            return true;
        }

        int itemCount = mComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            if (mComboBox.getItemAt(i).isModified()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void reset() {
        String translator = mSettings.getTranslator();
        int itemCount = mComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            TransPanel selectedPanel = mComboBox.getItemAt(i);
            if (Objects.equals(selectedPanel.getId(), translator)) {
                mComboBox.setSelectedIndex(i);
            }

            selectedPanel.reset();
        }
    }

    @Override
    public void apply() {
        TransPanel selectedPanel = (TransPanel) mComboBox.getSelectedItem();
        if (selectedPanel != null) {
            final String oldTranslator = mSettings.getTranslator();
            final String newTranslator = selectedPanel.getId();
            if (!oldTranslator.equals(newTranslator)) {
                mAppStorage.setLastSourceLanguage(null);
                mAppStorage.setLastTargetLanguage(null);
            }

            mSettings.setTranslator(newTranslator);
        }

        int itemCount = mComboBox.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            mComboBox.getItemAt(i).apply();
        }
    }

    interface TransPanel extends ConfigurablePanel {
        @NotNull
        String getId();

        @NotNull
        String getName();

        @NotNull
        Icon getIcon();
    }

}

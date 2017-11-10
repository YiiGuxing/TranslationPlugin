package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.AppStorage;
import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.util.SelectionMode;
import cn.yiiguxing.plugin.translate.util.StringsKt;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.FontComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.Objects;

/**
 * 设置页
 */
public class SettingsPanel implements ConfigurablePanel {

    private static final int INDEX_INCLUSIVE = 0;
    private static final int INDEX_EXCLUSIVE = 1;

    private JPanel mWholePanel;
    private JPanel mSelectionSettingsPanel;

    private JComboBox<String> mSelectionMode;
    private JPanel mHistoryPanel;
    private ComboBox mMaxHistoriesSize;
    private JButton mClearHistoriesButton;
    private JPanel mFontPanel;
    private JCheckBox mFontCheckBox;
    private FontComboBox mPrimaryFontComboBox;
    private FontComboBox mPhoneticFontComboBox;
    private JTextPane mFontPreview;
    private JLabel mPrimaryFontLabel;
    private JLabel mPhoneticFontLabel;
    private TransPanelContainer mTransPanelContainer;

    private final Settings mSettings;
    private final AppStorage mAppStorage;

    public SettingsPanel(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        super();

        mSettings = settings;
        mAppStorage = appStorage;

        setTitles();
        setRenderer();
        setListeners();
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return mWholePanel;
    }

    private void createUIComponents() {
        mTransPanelContainer = new TransPanelContainer(mSettings);

        mPrimaryFontComboBox = new FontComboBox(false, false);
        mPhoneticFontComboBox = new FontComboBox(false, true);

        fixFontComboBoxSize(mPrimaryFontComboBox);
        fixFontComboBoxSize(mPhoneticFontComboBox);
    }

    private void fixFontComboBoxSize(FontComboBox fontComboBox) {
        Dimension size = fontComboBox.getPreferredSize();
        size.width = size.height * 8;
        fontComboBox.setPreferredSize(size);
    }

    private void setTitles() {
        mSelectionSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("取词模式"));
        mFontPanel.setBorder(IdeBorderFactory.createTitledBorder("字体"));
        mHistoryPanel.setBorder(IdeBorderFactory.createTitledBorder("历史记录"));
    }

    private void setRenderer() {
        mSelectionMode.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                setText(value);
                if (index == INDEX_INCLUSIVE) {
                    setToolTipText("以最大范围取最近的所有词");
                } else if (index == INDEX_EXCLUSIVE) {
                    setToolTipText("取最近的单个词");
                }
            }
        });
    }

    private void setListeners() {
        mFontCheckBox.addItemListener(e -> {
            final boolean selected = mFontCheckBox.isSelected();
            mPrimaryFontComboBox.setEnabled(selected);
            mPhoneticFontComboBox.setEnabled(selected);
            mFontPreview.setEnabled(selected);
            mPrimaryFontLabel.setEnabled(selected);
            mPhoneticFontLabel.setEnabled(selected);
        });
        mPrimaryFontComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                previewPrimaryFont(mPrimaryFontComboBox.getFontName());
            }
        });
        mPhoneticFontComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                previewPhoneticFont(mPhoneticFontComboBox.getFontName());
            }
        });
        mClearHistoriesButton.addActionListener(e -> mAppStorage.clearHistories());
    }

    private void previewPrimaryFont(String primary) {
        if (StringsKt.isNullOrBlank(primary)) {
            mFontPreview.setFont(JBUI.Fonts.label(14));
        } else {
            mFontPreview.setFont(JBUI.Fonts.create(primary, 14));
        }
    }

    private void previewPhoneticFont(String primary) {
        final StyledDocument document = mFontPreview.getStyledDocument();

        Font font;
        if (StringsKt.isNullOrBlank(primary)) {
            font = JBUI.Fonts.label(14);
        } else {
            font = JBUI.Fonts.create(primary, 14);
        }

        final SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributeSet, font.getFamily());
        document.setCharacterAttributes(4, 41, attributeSet, true);
    }

    @NotNull
    private SelectionMode getAutoSelectionMode() {
        if (mSelectionMode.getSelectedIndex() == INDEX_INCLUSIVE) {
            return SelectionMode.INCLUSIVE;
        } else {
            return SelectionMode.EXCLUSIVE;
        }
    }

    private int getMaxHistorySize() {
        final Object size = mMaxHistoriesSize.getEditor().getItem();
        if (size instanceof String) {
            try {
                return Integer.parseInt((String) size);
            } catch (NumberFormatException e) {
                /*no-op*/
            }
        }

        return -1;
    }

    @Override
    public boolean isModified() {
        return mTransPanelContainer.isModified()
                || mSettings.getAutoSelectionMode() != getAutoSelectionMode()
                || mAppStorage.getMaxHistorySize() != getMaxHistorySize()
                || mSettings.isOverrideFont() != mFontCheckBox.isSelected()
                || !Objects.equals(mSettings.getPrimaryFontFamily(), mPrimaryFontComboBox.getFontName())
                || !Objects.equals(mSettings.getPhoneticFontFamily(), mPhoneticFontComboBox.getFontName());
    }

    @Override
    public void apply() {
        mTransPanelContainer.apply();

        final int maxHistorySize = getMaxHistorySize();
        if (maxHistorySize >= 0) {
            mAppStorage.setMaxHistorySize(maxHistorySize);
        }

        mSettings.setOverrideFont(mFontCheckBox.isSelected());
        mSettings.setPrimaryFontFamily(mPrimaryFontComboBox.getFontName());
        mSettings.setPhoneticFontFamily(mPhoneticFontComboBox.getFontName());
        mSettings.setAutoSelectionMode(getAutoSelectionMode());
    }

    @Override
    public void reset() {
        mTransPanelContainer.reset();

        mFontCheckBox.setSelected(mSettings.isOverrideFont());
        mPrimaryFontComboBox.setFontName(mSettings.getPrimaryFontFamily());
        mPhoneticFontComboBox.setFontName(mSettings.getPhoneticFontFamily());
        previewPrimaryFont(mSettings.getPrimaryFontFamily());
        previewPhoneticFont(mSettings.getPhoneticFontFamily());

        mMaxHistoriesSize.getEditor().setItem(Integer.toString(mAppStorage.getMaxHistorySize()));
        mSelectionMode.setSelectedIndex(mSettings.getAutoSelectionMode() == SelectionMode.INCLUSIVE
                ? INDEX_INCLUSIVE : INDEX_EXCLUSIVE);
    }
}

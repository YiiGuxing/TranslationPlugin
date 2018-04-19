package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.AppStorage;
import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.ui.settings.TranslatorSettingsContainer;
import cn.yiiguxing.plugin.translate.util.SelectionMode;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.FontComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * 设置页
 */
public class SettingsForm {

    private JPanel mWholePanel;
    private JPanel mSelectionSettingsPanel;
    private ComboBox<SelectionMode> mSelectionMode;
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
    private TranslatorSettingsContainer mTransPanelContainer;
    private JPanel mWindowOptionsPanel;
    private JCheckBox mShowStatusIconCheckBox;

    private final Settings mSettings;
    private final AppStorage mAppStorage;

    public SettingsForm(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        super();

        mSettings = settings;
        mAppStorage = appStorage;
    }

    private void createUIComponents() {
        mTransPanelContainer = new TranslatorSettingsContainer(mSettings);

        try {
            mPrimaryFontComboBox = new FontComboBox(false, false);
            mPhoneticFontComboBox = new FontComboBox(false, true);
        } catch (NoSuchMethodError e) {
            // Linux 下可能没有这构造函数
            try {
                mPrimaryFontComboBox = new FontComboBox(false);
                mPhoneticFontComboBox = new FontComboBox(false);
            } catch (NoSuchMethodError e1) {
                mPrimaryFontComboBox = new FontComboBox();
                mPhoneticFontComboBox = new FontComboBox();
            }
        }
    }


    @NotNull
    public final Settings getSettings() {
        return mSettings;
    }

    @NotNull
    public final AppStorage getAppStorage() {
        return mAppStorage;
    }

    @NotNull
    public final JPanel getWholePanel() {
        return mWholePanel;
    }

    @NotNull
    public final JPanel getSelectionSettingsPanel() {
        return mSelectionSettingsPanel;
    }

    @NotNull
    public final ComboBox<SelectionMode> getSelectionModeComboBox() {
        return mSelectionMode;
    }

    @NotNull
    public final JPanel getHistoryPanel() {
        return mHistoryPanel;
    }

    @NotNull
    public final ComboBox getMaxHistoriesSizeComboBox() {
        return mMaxHistoriesSize;
    }

    @NotNull
    public final JButton getClearHistoriesButton() {
        return mClearHistoriesButton;
    }

    @NotNull
    public final JPanel getFontPanel() {
        return mFontPanel;
    }

    @NotNull
    public final JCheckBox getFontCheckBox() {
        return mFontCheckBox;
    }

    @NotNull
    public final FontComboBox getPrimaryFontComboBox() {
        return mPrimaryFontComboBox;
    }

    @NotNull
    public final FontComboBox getPhoneticFontComboBox() {
        return mPhoneticFontComboBox;
    }

    @NotNull
    public final JTextPane getFontPreview() {
        return mFontPreview;
    }

    @NotNull
    public final JLabel getPrimaryFontLabel() {
        return mPrimaryFontLabel;
    }

    @NotNull
    public final JLabel getPhoneticFontLabel() {
        return mPhoneticFontLabel;
    }

    @NotNull
    public final TranslatorSettingsContainer getTransPanelContainer() {
        return mTransPanelContainer;
    }

    @NotNull
    public final JPanel getWindowOptionsPanel() {
        return mWindowOptionsPanel;
    }

    @NotNull
    public final JCheckBox getShowStatusIconCheckBox() {
        return mShowStatusIconCheckBox;
    }
}

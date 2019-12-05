package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.AppStorage;
import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.TTSSource;
import cn.yiiguxing.plugin.translate.TargetLanguageSelection;
import cn.yiiguxing.plugin.translate.ui.settings.TranslatorSettingsContainer;
import cn.yiiguxing.plugin.translate.util.LogsKt;
import cn.yiiguxing.plugin.translate.util.SelectionMode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.FontComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.labels.LinkLabel;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * 设置页
 */
public class SettingsForm {

    private static Logger LOGGER = Logger.getInstance(SettingsForm.class);

    private JPanel mWholePanel;
    private JPanel mTranslateSettingsPanel;
    private ComboBox<SelectionMode> mSelectionMode;
    private ComboBox<TargetLanguageSelection> mTargetLangSelectionComboBox;
    private JTextField mSeparatorsTextField;
    private EditorTextField mIgnoreRegExp;
    private JButton mCheckIgnoreRegExpButton;
    private JLabel mIgnoreRegExpMsg;
    private JPanel mHistoryPanel;
    private ComboBox mMaxHistoriesSize;
    private JButton mClearHistoriesButton;
    private JPanel mFontPanel;
    private JBCheckBox mFontCheckBox;
    private FontComboBox mPrimaryFontComboBox;
    private FontComboBox mPhoneticFontComboBox;
    private JLabel mPrimaryFontPreview;
    private JLabel mPhoneticFontPreview;
    private JLabel mPrimaryFontLabel;
    private JLabel mPhoneticFontLabel;
    private TranslatorSettingsContainer mTransPanelContainer;
    private JPanel mOptionsPanel;
    private JBCheckBox mShowStatusIconCheckBox;
    private JBCheckBox mFoldOriginalCheckBox;
    private JBCheckBox mKeepFormatCheckBox;
    private JBCheckBox mAutoPlayTTSCheckBox;
    private ComboBox<TTSSource> mTTSSourceComboBox;
    private JBCheckBox mShowWordFormsCheckBox;
    private JBCheckBox mAutoReplaceCheckBox;
    private JBCheckBox mSelectTargetLanguageCheckBox;
    private JBCheckBox mShowWordsOnStartupCheckBox;
    private JBCheckBox mShowExplanationCheckBox;
    private LinkLabel mSupportLinkLabel;

    private final Settings mSettings;
    private final AppStorage mAppStorage;

    public SettingsForm(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        super();

        mSettings = settings;
        mAppStorage = appStorage;
    }

    private void createUIComponents() {
        Project project = ProjectManager.getInstance().getDefaultProject();
        mTransPanelContainer = new TranslatorSettingsContainer(mSettings);
        mIgnoreRegExp = new EditorTextField("", project, RegExpLanguage.INSTANCE.getAssociatedFileType());

        mPrimaryFontComboBox = createFontComboBox(false);
        mPhoneticFontComboBox = createFontComboBox(true);
    }

    private static FontComboBox createFontComboBox(boolean filterNonLatin) {
        try {
            LogsKt.d(LOGGER, String.format("Try new FontComboBox(false, %b, false).", filterNonLatin));
            //noinspection JavaReflectionMemberAccess
            return FontComboBox.class
                    .getDeclaredConstructor(Boolean.TYPE, Boolean.TYPE, Boolean.TYPE)
                    .newInstance(false, filterNonLatin, false);
        } catch (NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            try {
                LogsKt.d(LOGGER, String.format("Try new FontComboBox(false, %b).", filterNonLatin));
                return FontComboBox.class
                        .getDeclaredConstructor(Boolean.TYPE, Boolean.TYPE)
                        .newInstance(false, filterNonLatin);
            } catch (NoSuchMethodException | InstantiationException |
                    IllegalAccessException | InvocationTargetException ex) {
                try {
                    LogsKt.d(LOGGER, "Try new FontComboBox(false).");
                    return new FontComboBox(false);
                } catch (NoSuchMethodError exc) {
                    LogsKt.d(LOGGER, "Try new FontComboBox().");
                    return new FontComboBox();
                }
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
    public final JPanel getTranslateSettingsPanel() {
        return mTranslateSettingsPanel;
    }

    @NotNull
    public final ComboBox<SelectionMode> getSelectionModeComboBox() {
        return mSelectionMode;
    }

    @NotNull
    public final ComboBox<TargetLanguageSelection> getTargetLangSelectionComboBox() {
        return mTargetLangSelectionComboBox;
    }

    @NotNull
    public final JTextField getSeparatorsTextField() {
        return mSeparatorsTextField;
    }

    @NotNull
    public final EditorTextField getIgnoreRegExp() {
        return mIgnoreRegExp;
    }

    @NotNull
    public final JButton getCheckIgnoreRegExpButton() {
        return mCheckIgnoreRegExpButton;
    }

    @NotNull
    public final JLabel getIgnoreRegExpMsg() {
        return mIgnoreRegExpMsg;
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
    public final JBCheckBox getFontCheckBox() {
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
    public final JLabel getPrimaryFontPreview() {
        return mPrimaryFontPreview;
    }

    @NotNull
    public final JLabel getPhoneticFontPreview() {
        return mPhoneticFontPreview;
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
    public final JPanel getOptionsPanel() {
        return mOptionsPanel;
    }

    @NotNull
    public final JBCheckBox getShowStatusIconCheckBox() {
        return mShowStatusIconCheckBox;
    }

    @NotNull
    public final JBCheckBox getFoldOriginalCheckBox() {
        return mFoldOriginalCheckBox;
    }

    @NotNull
    public final JBCheckBox getKeepFormatCheckBox() {
        return mKeepFormatCheckBox;
    }

    @NotNull
    public final JBCheckBox getAutoPlayTTSCheckBox() {
        return mAutoPlayTTSCheckBox;
    }

    @NotNull
    public final ComboBox<TTSSource> getTTSSourceComboBox() {
        return mTTSSourceComboBox;
    }

    @NotNull
    public final JBCheckBox getShowWordFormsCheckBox() {
        return mShowWordFormsCheckBox;
    }

    @NotNull
    public final JBCheckBox getShowWordsOnStartupCheckBox() {
        return mShowWordsOnStartupCheckBox;
    }

    @NotNull
    public final JBCheckBox getShowExplanationCheckBox() {
        return mShowExplanationCheckBox;
    }

    @NotNull
    public final JBCheckBox getAutoReplaceCheckBox() {
        return mAutoReplaceCheckBox;
    }

    @NotNull
    public final JBCheckBox getSelectTargetLanguageCheckBox() {
        return mSelectTargetLanguageCheckBox;
    }

    @NotNull
    public final LinkLabel getSupportLinkLabel() {
        return mSupportLinkLabel;
    }
}

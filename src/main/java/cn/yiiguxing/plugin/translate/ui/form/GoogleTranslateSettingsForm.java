package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.trans.Lang;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * GoogleApiPanel
 */
public class GoogleTranslateSettingsForm {
    private JPanel mContentPanel;
    private ComboBox<Lang> mPrimaryLanguage;
    private JCheckBox useTranslateGoogleComCheckBox;

    @NotNull
    public final JPanel getContentPanel() {
        return mContentPanel;
    }

    @NotNull
    public final ComboBox<Lang> getPrimaryLanguage() {
        return mPrimaryLanguage;
    }

    @NotNull
    public final JCheckBox getUseTranslateGoogleComCheckBox() {
        return useTranslateGoogleComCheckBox;
    }
}

package cn.yiiguxing.plugin.translate.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * TranslationDialogForm
 * <p>
 * Created by Yii.Guxing on 2017/12/27
 */
public class TranslationDialogForm {

    private JPanel mRootPanel;
    private TitlePanel mTitlePanel;
    private ComboBox<String> mInputComboBox;
    private JButton mTranslateButton;
    private JPanel mMainContentPanel;
    private JPanel mLanguagePanel;
    private ComboBox mTargetLangComboBox;
    private LinkLabel mSwitchButton;
    private ComboBox mSourceLangComboBox;
    private JPanel mContentContainer;
    private JEditorPane mMessage;

    @NotNull
    public final JComponent getComponent() {
        return mRootPanel;
    }

    @NotNull
    public final TitlePanel getTitlePanel() {
        return mTitlePanel;
    }

    @NotNull
    public final ComboBox<String> getInputComboBox() {
        return mInputComboBox;
    }

    @NotNull
    public final JButton getTranslateButton() {
        return mTranslateButton;
    }

    @NotNull
    public JPanel getMainContentPanel() {
        return mMainContentPanel;
    }

    @NotNull
    public JPanel getLanguagePanel() {
        return mLanguagePanel;
    }

    @NotNull
    public ComboBox getTargetLangComboBox() {
        return mTargetLangComboBox;
    }

    @NotNull
    public LinkLabel getSwitchButton() {
        return mSwitchButton;
    }

    @NotNull
    public ComboBox getSourceLangComboBox() {
        return mSourceLangComboBox;
    }

    @NotNull
    public final JPanel getContentContainer() {
        return mContentContainer;
    }

    @NotNull
    public final JEditorPane getMessagePane() {
        return mMessage;
    }

}

package cn.yiiguxing.plugin.translate.ui.form;

import cn.yiiguxing.plugin.translate.trans.Lang;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * TranslationDialogForm
 * <p>
 * Created by Yii.Guxing on 2017/12/27
 */
public abstract class TranslationDialogForm extends DialogWrapper {

    private JPanel mRootPanel;
    private TitlePanel mTitlePanel;
    private ComboBox<String> mInputComboBox;
    private JButton mTranslateButton;
    private JPanel mMainContentPanel;
    private JPanel mLanguagePanel;
    private ComboBox<Lang> mTargetLangComboBox;
    private LinkLabel mSwapButton;
    private ComboBox<Lang> mSourceLangComboBox;
    private JPanel mContentContainer;
    private JEditorPane mMessage;

    protected TranslationDialogForm(@Nullable Project project) {
        super(project);
    }

    @NotNull
    protected final JComponent getComponent() {
        return mRootPanel;
    }

    @NotNull
    protected final TitlePanel getTitlePanel() {
        return mTitlePanel;
    }

    @NotNull
    protected final ComboBox<String> getInputComboBox() {
        return mInputComboBox;
    }

    @NotNull
    protected final JButton getTranslateButton() {
        return mTranslateButton;
    }

    @NotNull
    protected JPanel getMainContentPanel() {
        return mMainContentPanel;
    }

    @NotNull
    public JPanel getLanguagePanel() {
        return mLanguagePanel;
    }

    @NotNull
    protected ComboBox<Lang> getTargetLangComboBox() {
        return mTargetLangComboBox;
    }

    @NotNull
    protected LinkLabel getSwapButton() {
        return mSwapButton;
    }

    @NotNull
    protected ComboBox<Lang> getSourceLangComboBox() {
        return mSourceLangComboBox;
    }

    @NotNull
    protected final JPanel getContentContainer() {
        return mContentContainer;
    }

    @NotNull
    protected final JEditorPane getMessagePane() {
        return mMessage;
    }

}

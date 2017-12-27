package cn.yiiguxing.plugin.translate.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.TitlePanel;
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
    private JPanel mContentPanel;
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
    public final JPanel getContentPanel() {
        return mContentPanel;
    }

    @NotNull
    public final JEditorPane getMessagePane() {
        return mMessage;
    }

}

package cn.yiiguxing.plugin.translate.ui.form;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * TranslatorSettingsContainerForm
 */
public class TranslatorSettingsContainerForm<T> extends JPanel {

    private JPanel mRootPanel;
    private JPanel mContentPanel;
    private ComboBox<T> mComboBox;

    public TranslatorSettingsContainerForm() {
        super(new BorderLayout());
        add(mRootPanel);
    }

    @NotNull
    public final JPanel getContentPanel() {
        return mContentPanel;
    }

    @NotNull
    public final ComboBox<T> getComboBox() {
        return mComboBox;
    }
}

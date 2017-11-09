package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.trans.Lang;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;

/**
 * GoogleApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
public class GoogleApiPanel implements ConfigurablePanel.TranslateApiPanel {
    private JPanel mContentPanel;
    private ComboBox<Lang> mPrimaryLanguage;

    @NotNull
    @Override
    public String getId() {
        return "Google";
    }

    @NotNull
    @Override
    public String getTitle() {
        return "Google翻译";
    }

    private void createUIComponents() {
        mPrimaryLanguage = new ComboBox<>(new CollectionComboBoxModel<>(Arrays.asList(Lang.values())));
        mPrimaryLanguage.setRenderer(LanguageRenderer.INSTANCE);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mContentPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void apply() {

    }
}

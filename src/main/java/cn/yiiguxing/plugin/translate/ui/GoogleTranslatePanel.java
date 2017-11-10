package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.GoogleTranslateSettings;
import cn.yiiguxing.plugin.translate.trans.GoogleTranslator;
import cn.yiiguxing.plugin.translate.trans.Lang;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

/**
 * GoogleApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
@SuppressWarnings("WeakerAccess")
public class GoogleTranslatePanel implements TransPanelContainer.TransPanel {
    private JPanel mContentPanel;
    private ComboBox<Lang> mPrimaryLanguage;

    private final GoogleTranslateSettings mSettings;

    public GoogleTranslatePanel(@NotNull GoogleTranslateSettings settings) {
        mSettings = settings;
    }

    @NotNull
    @Override
    public String getId() {
        return GoogleTranslator.TRANSLATOR_ID;
    }

    @NotNull
    @Override
    public String getName() {
        return "Google翻译";
    }

    private void createUIComponents() {
        List<Lang> languages = GoogleTranslator.INSTANCE.getSupportedTargetLanguages();
        mPrimaryLanguage = new ComboBox<>(new CollectionComboBoxModel<>(languages));
        mPrimaryLanguage.setRenderer(LanguageRenderer.INSTANCE);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mContentPanel;
    }

    @Override
    public boolean isModified() {
        return Objects.equals(mPrimaryLanguage.getSelectedItem(), mSettings.getPrimaryLanguage());
    }

    @Override
    public void reset() {
        mPrimaryLanguage.setSelectedItem(mSettings.getPrimaryLanguage());
    }

    @Override
    public void apply() {
        Lang selectedLang = (Lang) mPrimaryLanguage.getSelectedItem();
        if (selectedLang != null) {
            mSettings.setPrimaryLanguage(selectedLang);
        }
    }
}

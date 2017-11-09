package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.ConstantsKt;
import cn.yiiguxing.plugin.translate.YoudaoTranslateSettings;
import cn.yiiguxing.plugin.translate.trans.Lang;
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

/**
 * YoudaoApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
@SuppressWarnings("WeakerAccess")
public class YoudaoTranslatePanel implements TransPanelContainer.TransPanel {

    private JPanel mContentPanel;
    private ComboBox<Lang> mPrimaryLanguage;
    private JBTextField mAppIdField;
    private JBPasswordField mAppKeyField;
    @SuppressWarnings("FieldCanBeLocal")
    private LinkLabel mGetApiKeyLink;

    private final YoudaoTranslateSettings mSettings;

    public YoudaoTranslatePanel(@NotNull YoudaoTranslateSettings settings) {
        this.mSettings = settings;
    }

    @NotNull
    @Override
    public String getId() {
        return YoudaoTranslator.TRANSLATOR_ID;
    }

    @NotNull
    @Override
    public String getName() {
        return "有道翻译";
    }

    private void createUIComponents() {
        mPrimaryLanguage = new ComboBox<>(new CollectionComboBoxModel<>(YoudaoTranslator.SUPPORTED_LANGUAGES));
        mPrimaryLanguage.setRenderer(LanguageRenderer.INSTANCE);

        mGetApiKeyLink = new ActionLink("", new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                BrowserUtil.browse(ConstantsKt.YOUDAO_AI_URL);
            }
        });
        mGetApiKeyLink.setIcon(AllIcons.Ide.Link);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
        return mContentPanel;
    }

    @NotNull
    private String getAppKey() {
        char[] password = mAppKeyField.getPassword();
        return password == null || password.length == 0 ? "" : new String(password);
    }

    private void setAppKey(@NotNull String key) {
        mAppKeyField.setText(key.isEmpty() ? null : key);
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(mAppIdField.getText(), mSettings.getAppId())
                || !Objects.equals(getAppKey(), mSettings.getAppKey())
                || !Objects.equals(mPrimaryLanguage.getSelectedItem(), mSettings.getPrimaryLanguage());
    }

    @Override
    public void reset() {
        setAppKey(mSettings.getAppKey());
        mAppIdField.setText(mSettings.getAppId());
        mPrimaryLanguage.setSelectedItem(mSettings.getPrimaryLanguage());
    }

    @Override
    public void apply() {
        mSettings.setAppId(mAppIdField.getText());
        mSettings.setAppKey(getAppKey());

        Lang selectedLang = (Lang) mPrimaryLanguage.getSelectedItem();
        if (selectedLang != null) {
            mSettings.setPrimaryLanguage(selectedLang);
        }
    }
}

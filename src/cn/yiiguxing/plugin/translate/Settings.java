package cn.yiiguxing.plugin.translate;

import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * 设置
 */
@SuppressWarnings("WeakerAccess")
public class Settings implements Configurable, ItemListener {

    private static final String URL = "http://fanyi.youdao.com/openapi?path=data-mode";

    private static final String API_KEY_NAME = "TranslationPlugin.API_KEY_NAME";
    private static final String API_KEY_VALUE = "TranslationPlugin.API_KEY_VALUE";
    private static final String API_KEY_USER_DEFAULT = "TranslationPlugin.API_KEY_USER_DEFAULT";

    private static final boolean DEFAULT_USER_DEFAULT_KEY = true;

    private JPanel contentPane;
    @SuppressWarnings("unused")
    private LinkLabel linkLabel;
    private JTextField keyNameField;
    private JTextField keyValueField;
    private JCheckBox checkBox;

    @Nls
    @Override
    public String getDisplayName() {
        return "Translation";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        checkBox.addItemListener(this);
        return contentPane;
    }

    private void createUIComponents() {
        linkLabel = new ActionLink("", AllIcons.Ide.Link, new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                obtainApiKey();
            }
        });
    }

    private static void obtainApiKey() {
        WebBrowser browser = WebBrowserManager.getInstance().getFirstActiveBrowser();
        if (browser != null) {
            BrowserLauncher.getInstance().browseUsingPath(URL, null, browser, null, ArrayUtil.EMPTY_STRING_ARRAY);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        switchKey();
    }

    private void switchKey() {
        if (checkBox.isSelected()) {
            useDefaultKey();
        } else {
            useCustomKey();
        }
    }

    private void useDefaultKey() {
        if (Utils.isEmptyOrBlankString(keyNameField.getText())
                && Utils.isEmptyOrBlankString(keyValueField.getText())) {
            PropertiesComponent component = PropertiesComponent.getInstance();
            component.setValue(API_KEY_NAME, null);
            component.setValue(API_KEY_VALUE, null);
        }

        keyNameField.setText("Default");
        keyNameField.setEnabled(false);
        keyValueField.setText("Default");
        keyValueField.setEnabled(false);
    }

    private void useCustomKey() {
        PropertiesComponent component = PropertiesComponent.getInstance();

        keyNameField.setText(component.getValue(API_KEY_NAME, ""));
        keyNameField.setEnabled(true);
        keyValueField.setText(component.getValue(API_KEY_VALUE, ""));
        keyValueField.setEnabled(true);
    }

    @Override
    public boolean isModified() {
        return !Utils.isEmptyOrBlankString(keyNameField.getText())
                && !Utils.isEmptyOrBlankString(keyValueField.getText());
    }

    @Override
    public void apply() throws ConfigurationException {
        PropertiesComponent component = PropertiesComponent.getInstance();

        boolean validKey = !Utils.isEmptyOrBlankString(keyNameField.getText())
                && !Utils.isEmptyOrBlankString(keyValueField.getText());
        boolean useDefault = checkBox.isSelected();
        if (!useDefault) {
            component.setValue(API_KEY_NAME, keyNameField.getText());
            component.setValue(API_KEY_VALUE, keyValueField.getText());
        }

        component.setValue(API_KEY_USER_DEFAULT, useDefault || !validKey, DEFAULT_USER_DEFAULT_KEY);
    }

    @Override
    public void reset() {
        checkBox.setSelected(isUseDefaultKey());
    }

    @Override
    public void disposeUIResources() {
        checkBox.removeItemListener(this);
    }

    public static boolean isUseDefaultKey() {
        return PropertiesComponent.getInstance().getBoolean(API_KEY_USER_DEFAULT, DEFAULT_USER_DEFAULT_KEY);
    }

    public static String getApiKeyName() {
        return PropertiesComponent.getInstance().getValue(API_KEY_NAME, "");
    }

    public static String getApiKeyValue() {
        return PropertiesComponent.getInstance().getValue(API_KEY_VALUE, "");
    }

}

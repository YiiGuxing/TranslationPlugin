package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.action.AutoSelectionMode;
import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * 设置
 */
@SuppressWarnings("WeakerAccess")
public class SettingsConfigurable implements Configurable, ItemListener {

    private static final String URL = "http://fanyi.youdao.com/openapi?path=data-mode";

    private static final int INDEX_INCLUSIVE = 0;
    private static final int INDEX_EXCLUSIVE = 1;

    private final Settings settings;

    private JPanel contentPane;
    @SuppressWarnings("unused")
    private LinkLabel linkLabel;
    private JTextField keyNameField;
    private JTextField keyValueField;
    private JCheckBox checkBox;
    @SuppressWarnings("Since15")
    private JComboBox<String> autoSelectionMode;

    public SettingsConfigurable() {
        settings = Settings.getInstance();
    }

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
        autoSelectionMode.setRenderer(new ListCellRendererWrapper<String>() {
            @Override
            public void customize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                setText(value);
                if (index == INDEX_INCLUSIVE) {
                    setToolTipText("以最大范围取最近的所有词");
                } else if (index == INDEX_EXCLUSIVE) {
                    setToolTipText("取最近的单个词");
                }
            }
        });
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
            settings.setApiKeyName(null);
            settings.setApiKeyValue(null);
        }

        keyNameField.setText("Default");
        keyNameField.setEnabled(false);
        keyValueField.setText("Default");
        keyValueField.setEnabled(false);
    }

    private void useCustomKey() {
        keyNameField.setText(settings.getApiKeyName());
        keyNameField.setEnabled(true);
        keyValueField.setText(settings.getApiKeyValue());
        keyValueField.setEnabled(true);
    }

    @NotNull
    private AutoSelectionMode getAutoSelectionMode() {
        if (autoSelectionMode.getSelectedIndex() == INDEX_INCLUSIVE) {
            return AutoSelectionMode.INCLUSIVE;
        } else {
            return AutoSelectionMode.EXCLUSIVE;
        }
    }

    @Override
    public boolean isModified() {
        return (!Utils.isEmptyOrBlankString(keyNameField.getText())
                && !Utils.isEmptyOrBlankString(keyValueField.getText()))
                || (settings.getAutoSelectionMode() != getAutoSelectionMode());
    }

    @Override
    public void apply() throws ConfigurationException {
        boolean validKey = !Utils.isEmptyOrBlankString(keyNameField.getText())
                && !Utils.isEmptyOrBlankString(keyValueField.getText());
        boolean useDefault = checkBox.isSelected();
        if (!useDefault) {
            settings.setApiKeyName(keyNameField.getText());
            settings.setApiKeyValue(keyValueField.getText());
        }

        settings.setUseDefaultKey(useDefault || !validKey);
        settings.setAutoSelectionMode(getAutoSelectionMode());
    }

    @Override
    public void reset() {
        checkBox.setSelected(settings.isUseDefaultKey());
        autoSelectionMode.setSelectedIndex(settings.getAutoSelectionMode() == AutoSelectionMode.INCLUSIVE
                ? INDEX_INCLUSIVE : INDEX_EXCLUSIVE);
    }

    @Override
    public void disposeUIResources() {
        checkBox.removeItemListener(this);
    }

}

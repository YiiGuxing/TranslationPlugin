package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.AppStorage;
import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.action.AutoSelectionMode;
import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.ide.browsers.WebBrowser;
import com.intellij.ide.browsers.WebBrowserManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * 设置页
 */
@SuppressWarnings("Since15")
public class SettingsPanel {

    private static final String API_KEY_URL = "http://fanyi.youdao.com/openapi?path=data-mode";

    private static final int INDEX_INCLUSIVE = 0;
    private static final int INDEX_EXCLUSIVE = 1;

    private JPanel mWholePanel;
    private JPanel mSelectionSettingsPanel;
    private JPanel mApiKeySettingsPanel;

    private JComboBox<String> mSelectionMode;
    private JTextField mKeyNameField;
    private JTextField mKeyValueField;
    private JCheckBox mDefaultApiKey;
    private LinkLabel mGetApiKeyLink;
    private JLabel mMessage;
    private JPanel mHistoryPanel;
    private ComboBox mMaxHistoriesSize;
    private JButton mClearHistoriesButton;

    private Settings mSettings;
    private AppStorage mAppStorage;

    public JComponent createPanel(@NotNull Settings settings, @NotNull AppStorage appStorage) {
        mSettings = settings;
        mAppStorage = appStorage;

        mSelectionSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("取词模式"));
        mHistoryPanel.setBorder(IdeBorderFactory.createTitledBorder("历史记录"));
        mApiKeySettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("有道 API KEY"));

        mSelectionMode.setRenderer(new ListCellRendererWrapper<String>() {
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

        mDefaultApiKey.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                switchKey();
            }
        });
        mClearHistoriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mAppStorage.clearHistories();
            }
        });

        return mWholePanel;
    }

    private void createUIComponents() {
        mGetApiKeyLink = new ActionLink("", new AnAction() {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                obtainApiKey();
            }
        });
        mGetApiKeyLink.setIcon(AllIcons.Ide.Link);
    }

    private static void obtainApiKey() {
        WebBrowser browser = WebBrowserManager.getInstance().getFirstActiveBrowser();
        if (browser != null) {
            BrowserLauncher.getInstance()
                    .browseUsingPath(API_KEY_URL, null, browser, null, ArrayUtil.EMPTY_STRING_ARRAY);
        }
    }

    private void switchKey() {
        if (mDefaultApiKey.isSelected()) {
            useDefaultKey();
        } else {
            useCustomKey();
        }
    }

    private void useDefaultKey() {
        if (Utils.isEmptyOrBlankString(mKeyNameField.getText())
                && Utils.isEmptyOrBlankString(mKeyValueField.getText())) {
            mSettings.setApiKeyName(null);
            mSettings.setApiKeyValue(null);
        }

        mKeyNameField.setText("Default");
        mKeyNameField.setEnabled(false);
        mKeyValueField.setText("Default");
        mKeyValueField.setEnabled(false);
        mMessage.setVisible(true);
    }

    private void useCustomKey() {
        mKeyNameField.setText(mSettings.getApiKeyName());
        mKeyNameField.setEnabled(true);
        mKeyValueField.setText(mSettings.getApiKeyValue());
        mKeyValueField.setEnabled(true);
        mMessage.setVisible(false);
    }

    @NotNull
    private AutoSelectionMode getAutoSelectionMode() {
        if (mSelectionMode.getSelectedIndex() == INDEX_INCLUSIVE) {
            return AutoSelectionMode.INCLUSIVE;
        } else {
            return AutoSelectionMode.EXCLUSIVE;
        }
    }

    private int getMaxHistorySize() {
        final Object size = mMaxHistoriesSize.getEditor().getItem();
        if (size instanceof Integer) {
            return (Integer) size;
        } else if (size instanceof String) {
            try {
                return Integer.parseInt((String) size);
            } catch (NumberFormatException e) {
                /*no-op*/
            }
        }

        return -1;
    }

    public boolean isModified() {
        return (!Utils.isEmptyOrBlankString(mKeyNameField.getText())
                && !Utils.isEmptyOrBlankString(mKeyValueField.getText()))
                || (mSettings.getAutoSelectionMode() != getAutoSelectionMode())
                || (getMaxHistorySize() != mAppStorage.getMaxHistorySize());
    }

    public void apply() {
        final int maxHistorySize = getMaxHistorySize();
        if (maxHistorySize >= 0) {
            mAppStorage.setMaxHistoriesSize(maxHistorySize);
        }

        boolean validKey = !Utils.isEmptyOrBlankString(mKeyNameField.getText())
                && !Utils.isEmptyOrBlankString(mKeyValueField.getText());
        boolean useDefault = mDefaultApiKey.isSelected();
        if (!useDefault) {
            mSettings.setApiKeyName(mKeyNameField.getText());
            mSettings.setApiKeyValue(mKeyValueField.getText());
        }

        mSettings.setUseDefaultKey(useDefault || !validKey);
        mSettings.setAutoSelectionMode(getAutoSelectionMode());
    }

    public void reset() {
        mMaxHistoriesSize.getEditor().setItem(mAppStorage.getMaxHistorySize());
        mDefaultApiKey.setSelected(mSettings.isUseDefaultKey());
        mSelectionMode.setSelectedIndex(mSettings.getAutoSelectionMode() == AutoSelectionMode.INCLUSIVE
                ? INDEX_INCLUSIVE : INDEX_EXCLUSIVE);
    }
}

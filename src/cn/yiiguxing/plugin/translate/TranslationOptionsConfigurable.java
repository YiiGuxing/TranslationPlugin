package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.ui.SettingsPanel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 选项配置
 */
public class TranslationOptionsConfigurable implements SearchableConfigurable, Configurable.NoScroll, Disposable {

    private final Settings mSettings;

    private SettingsPanel mPanel;

    public TranslationOptionsConfigurable() {
        mSettings = Settings.getInstance();
    }

    @NotNull
    @Override
    public String getId() {
        return "yiiguxing.plugin.translate";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
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
        mPanel = new SettingsPanel();
        return mPanel.createPanel(mSettings);
    }

    @Override
    public boolean isModified() {
        return mPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        mPanel.apply();
    }

    @Override
    public void reset() {
        mPanel.reset();
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(this);
    }

    @Override
    public void dispose() {
        mPanel = null;
    }

    public static void showSettingsDialog(Project project) {
        if (project == null)
            return;
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TranslationOptionsConfigurable.class);
    }

}

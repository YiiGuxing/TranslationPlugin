package cn.yiiguxing.plugin.translate.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * GoogleApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
public class GoogleApiPanel implements ConfigurablePanel.TranslateApiPanel {
    private JPanel mContentPanel;

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

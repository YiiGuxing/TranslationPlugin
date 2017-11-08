package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * YoudaoApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
public class YoudaoApiPanel implements ConfigurablePanel.TranslateApiPanel {

    private JPanel mContentPanel;

    @NotNull
    @Override
    public String getId() {
        return YoudaoTranslator.TRANSLATOR_ID;
    }

    @NotNull
    @Override
    public String getTitle() {
        return "有道翻译";
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

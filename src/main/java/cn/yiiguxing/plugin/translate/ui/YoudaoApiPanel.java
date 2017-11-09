package cn.yiiguxing.plugin.translate.ui;

import cn.yiiguxing.plugin.translate.ConstantsKt;
import cn.yiiguxing.plugin.translate.trans.Lang;
import cn.yiiguxing.plugin.translate.trans.YoudaoTranslator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.ui.components.labels.LinkLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * YoudaoApiPanel
 * <p>
 * Created by Yii.Guxing on 2017/11/8
 */
public class YoudaoApiPanel implements ConfigurablePanel.TranslateApiPanel {

    private JPanel mContentPanel;
    private ComboBox<Lang> mPrimaryLanguage;
    private LinkLabel mGetApiKeyLink;

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

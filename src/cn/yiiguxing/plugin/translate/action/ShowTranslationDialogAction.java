package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.ui.Icons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;

public class ShowTranslationDialogAction extends AnAction implements DumbAware {

    public ShowTranslationDialogAction() {
        super(Icons.Translate);
        setEnabledInModalContext(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment())
            return;

        TranslationUiManager.getInstance().showTranslationDialog(e.getProject());
    }

}

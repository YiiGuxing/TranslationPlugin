package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.ui.Icons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;

public class OpenTranslationDialogAction extends AnAction implements DumbAware {

    public OpenTranslationDialogAction() {
        super(Icons.Translate);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        TranslationUiManager.getInstance().showTranslationDialog(e.getProject());
    }

}

package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.IconLoader;

public class TranslationAction extends AnAction implements DumbAware {

    public TranslationAction() {
        super("TranslationDialog", "Translate", IconLoader.getIcon("/icon_16.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        TranslationDialogManager.getInstance().show(e.getProject());
    }

}

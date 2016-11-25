package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.Utils;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;

/**
 * 翻译快速文档中选中的文本
 */
public class TranslateQuickDocAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

    public TranslateQuickDocAction() {
        setEnabledInModalContext(true);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null || ApplicationManager.getApplication().isHeadlessEnvironment())
            return;

        final JBPopup docInfoHint = DocumentationManager.getInstance(project).getDocInfoHint();
        if (docInfoHint != null) {
            docInfoHint.dispose();
        }

        final String selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT);
        if (!Utils.isEmptyOrBlankString(selected)) {
            TranslationUiManager.getInstance().showTranslationDialog(project).query(selected);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        String selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT);
        e.getPresentation().setEnabled(!Utils.isEmptyOrBlankString(selected));
    }
}

package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.Utils;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.JTextComponent;

/**
 * 文本组件（如快速文档、提示气泡、输入框……）翻译动作
 */
public class TranslateTextComponentAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

    public TranslateTextComponentAction() {
        setEnabledInModalContext(true);
    }

    @Nullable
    public static String getSelectedText(@NotNull AnActionEvent event) {
        final DataContext dataContext = event.getDataContext();

        String selectedQuickDocText = DocumentationManager.SELECTED_QUICK_DOC_TEXT.getData(dataContext);
        if (selectedQuickDocText != null) {
            return selectedQuickDocText;
        }

        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) {
            return editor.getSelectionModel().getSelectedText();
        }

        final Object data = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
        if (data instanceof JTextComponent) {
            return ((JTextComponent) data).getSelectedText();
        }

        return null;
    }

    @Override
    public void update(AnActionEvent e) {
        final String selected = getSelectedText(e);
        e.getPresentation().setEnabledAndVisible(!Utils.isEmptyOrBlankString(Utils.splitWord(selected)));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            return;
        }

        final String selected = Utils.splitWord(getSelectedText(e));
        if (Utils.isEmptyOrBlankString(selected)) {
            return;
        }

        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        final JBPopup docInfoHint = project == null ? null : DocumentationManager.getInstance(project).getDocInfoHint();
        if (docInfoHint != null) {
            docInfoHint.cancel();
        }

        TranslationUiManager.getInstance().showTranslationDialog(e.getProject()).query(selected);
    }

}

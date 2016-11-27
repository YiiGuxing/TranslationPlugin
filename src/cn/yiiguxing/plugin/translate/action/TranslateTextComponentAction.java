package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.Utils;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.JTextComponent;

public class TranslateTextComponentAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

    public TranslateTextComponentAction() {
        setEnabledInModalContext(true);

        ActionManager.getInstance().addAnActionListener(new AnActionListener.Adapter() {
            @Override
            public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                System.out.println(action.getClass().getClass().getCanonicalName());
                System.out.println(action);
                System.out.println(dataContext);
            }
        });
    }

    @Nullable
    public static Editor getEditorFromContext(@NotNull DataContext dataContext) {
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null) return editor;

        final Object data = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
        if (data instanceof JTextComponent) {
            return new TextComponentEditorImpl(CommonDataKeys.PROJECT.getData(dataContext), (JTextComponent) data);
        }

        return null;
    }

    @Override
    public void update(AnActionEvent e) {
        final String selected = getSelectedText(e);
        e.getPresentation().setEnabledAndVisible(!Utils.isEmptyOrBlankString(Utils.splitWord(selected)));
    }

    @Nullable
    private String getSelectedText(AnActionEvent e) {
        String selected = e.getData(DocumentationManager.SELECTED_QUICK_DOC_TEXT);
        if (selected == null) {
            final Editor editor = getEditorFromContext(e.getDataContext());
            if (editor != null) {
                selected = editor.getSelectionModel().getSelectedText();
            }
        }

        return selected;
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
            docInfoHint.dispose();
        }

        TranslationUiManager.getInstance().showTranslationDialog(e.getProject()).query(selected);
    }

}

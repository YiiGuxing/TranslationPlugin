package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

public class TranslateAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);

        TranslateDialog translateDialog = new TranslateDialog();
        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            translateDialog.query(selectedText);
        }
        translateDialog.setVisible(true);
    }

    @Nullable
    protected Editor getEditor(AnActionEvent e) {
        return EDITOR.getData(e.getDataContext());
    }

}

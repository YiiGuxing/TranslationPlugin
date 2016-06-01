package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR;

public class TranslationAction extends AnAction implements DumbAware {

    public TranslationAction() {
        super("Translation", "Translation", IconLoader.getIcon("/icon_16.png"));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);

        TranslationDialog translationDialog = new TranslationDialog();
        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            translationDialog.query(selectedText);
        }
        translationDialog.setVisible(true);
    }

    @Nullable
    private Editor getEditor(AnActionEvent e) {
        return EDITOR.getData(e.getDataContext());
    }

}

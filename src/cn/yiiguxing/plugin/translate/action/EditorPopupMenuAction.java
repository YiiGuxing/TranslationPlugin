package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.ui.Icons;
import cn.yiiguxing.plugin.translate.ui.TranslationBalloon;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;

public class EditorPopupMenuAction extends AnAction implements DumbAware {

    public EditorPopupMenuAction() {
        super("Translate", "Translate", Icons.Translate);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        String selectedText = Utils.splitWord(getSelectedText(e));
        if (editor != null && !Utils.isEmptyOrBlankString(selectedText)) {
            new TranslationBalloon(editor).showAndQuery(selectedText);
        }
    }

    private Editor getEditor(AnActionEvent e) {
        return CommonDataKeys.EDITOR.getData(e.getDataContext());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(!Utils.isEmptyOrBlankString(getSelectedText(e)));
    }

    private String getSelectedText(AnActionEvent e) {
        Editor editor = getEditor(e);
        String selectedText = null;
        if (editor != null) {
            selectedText = editor.getSelectionModel().getSelectedText();
        }

        return selectedText;
    }

}

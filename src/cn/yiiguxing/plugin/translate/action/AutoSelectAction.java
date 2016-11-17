package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.compat.SelectWordUtilCompat;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;

abstract class AutoSelectAction extends AnAction {

    private final boolean mCheckSelection;

    public AutoSelectAction(boolean checkSelection) {
        this(null, null, null, checkSelection);
    }

    public AutoSelectAction(Icon icon, boolean checkSelection) {
        this(null, null, icon, checkSelection);
    }

    public AutoSelectAction(@Nullable String text, boolean checkSelection) {
        this(text, null, null, checkSelection);
    }

    public AutoSelectAction(@Nullable String text, @Nullable String description, @Nullable Icon icon, boolean checkSelection) {
        super(text, description, icon);
        this.mCheckSelection = checkSelection;
    }

    /**
     * 返回取词模式
     */
    @NotNull
    protected abstract AutoSelectionMode getAutoSelectionMode();

    protected void onUpdate(AnActionEvent e, boolean active) {
    }

    protected void onActionPerformed(@NotNull Editor editor, @NotNull TextRange selectionRange) {
    }

    @Override
    public final void update(AnActionEvent e) {
        boolean active = false;
        Editor editor = getEditor(e);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            active = (mCheckSelection && selectionModel.hasSelection()) || canSelect(editor);
        }

        onUpdate(e, active);
    }

    @Override
    public final void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;

        final Editor editor = getEditor(e);
        if (editor != null) {
            TextRange selectionRange = getSelectionRange(e);
            if (selectionRange != null && !selectionRange.isEmpty()) {
                onActionPerformed(editor, selectionRange);
            }
        }
    }

    private boolean canSelect(Editor editor) {
        final int offset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        int textLength = document.getTextLength();

        if (textLength == 0)
            return false;

        String text = document.getText(new TextRange(Math.max(0, offset - 1), Math.min(textLength, offset + 1)));
        for (int i = 0; i < text.length(); i++) {
            if (Character.isJavaIdentifierPart(text.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private Editor getEditor(AnActionEvent e) {
        return CommonDataKeys.EDITOR.getData(e.getDataContext());
    }

    @Nullable
    private TextRange getSelectionRange(AnActionEvent e) {
        TextRange selectionRange = null;

        Editor editor = getEditor(e);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            if (mCheckSelection && selectionModel.hasSelection()) {
                selectionRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            } else {
                final ArrayList<TextRange> ranges = new ArrayList<TextRange>();
                final int offset = editor.getCaretModel().getOffset();

                final AutoSelectionMode selectionMode = Utils.requireNonNull(getAutoSelectionMode(),
                        "Method getAutoSelectionMode() can not return null.");
                final boolean exclusiveMode = selectionMode == AutoSelectionMode.EXCLUSIVE;

                SelectWordUtilCompat.addWordOrLexemeSelection(exclusiveMode, editor, offset, ranges);

                if (!ranges.isEmpty()) {
                    if (exclusiveMode) {
                        selectionRange = ranges.get(0);
                    } else {
                        TextRange maxRange = null;
                        for (TextRange range : ranges) {
                            if (maxRange == null || range.contains(maxRange)) {
                                maxRange = range;
                            }
                        }

                        selectionRange = maxRange;
                    }
                }
            }
        }

        return selectionRange;
    }

}

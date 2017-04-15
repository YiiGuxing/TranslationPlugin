package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.compat.SelectWordUtilCompat;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
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

/**
 * 自动从光标周围取词
 */
@SuppressWarnings("WeakerAccess")
abstract class AutoSelectAction extends AnAction {

    private final SelectWordUtil.CharCondition mWordPartCondition;
    private final boolean mCheckSelection;

    public AutoSelectAction(@Nullable SelectWordUtil.CharCondition isWordPartCondition, boolean checkSelection) {
        this(null, null, null, isWordPartCondition, checkSelection);
    }

    public AutoSelectAction(Icon icon, boolean checkSelection) {
        this(null, null, icon, null, checkSelection);
    }

    public AutoSelectAction(@Nullable String text,
                            @Nullable String description,
                            @Nullable Icon icon,
                            @Nullable SelectWordUtil.CharCondition isWordPartCondition,
                            boolean checkSelection) {
        super(text, description, icon);
        setEnabledInModalContext(true);
        this.mWordPartCondition = isWordPartCondition != null
                ? isWordPartCondition : SelectWordUtilCompat.DEFAULT_CONDITION;
        this.mCheckSelection = checkSelection;
    }

    /**
     * 返回取词模式
     */
    @NotNull
    protected abstract AutoSelectionMode getAutoSelectionMode();

    /**
     * 更新Action
     *
     * @param e      事件
     * @param active 是否活动的，表示是否可以取到词
     */
    protected void onUpdate(AnActionEvent e, boolean active) {
    }

    /**
     * 执行操作
     *
     * @param e              事件
     * @param editor         编辑器
     * @param selectionRange 取词的范围
     */
    protected void onActionPerformed(AnActionEvent e, @NotNull Editor editor, @NotNull TextRange selectionRange) {
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
                onActionPerformed(e, editor, selectionRange);
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
            if (mWordPartCondition.value(text.charAt(i))) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    protected static Editor getEditor(AnActionEvent e) {
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

                SelectWordUtilCompat.addWordOrLexemeSelection(exclusiveMode, editor, offset, ranges, mWordPartCondition);

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

package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.compat.SelectWordUtilCompat;
import cn.yiiguxing.plugin.translate.ui.Icons;
import cn.yiiguxing.plugin.translate.ui.TranslationBalloon;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

public class TranslateAction extends AnAction implements DumbAware {

    private static final TextAttributes HIGHLIGHT_ATTRIBUTES;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(new JBColor(new Color(0xFFE4E4FF), new Color(0xFF344134)));
        attributes.setEffectType(EffectType.LINE_UNDERSCORE);
        attributes.setEffectColor(new JBColor(0xFFEE6000, 0xFFCC7832));

        HIGHLIGHT_ATTRIBUTES = attributes;
    }

    @Nullable
    private final AutoSelectionMode mAutoSelectionMode;
    private final boolean mCheckSelection;

    @Nullable
    private TextRange mQueryTextRange;

    /**
     * @param autoSelectionMode 取词模式
     * @param checkSelection    指定是否检查手动选择的文本。<code>true</code> - 如果有手动选择文本，
     *                          则忽略<code>autoSelectionMode</code>, <code>false</code> - 将忽略手动选择的文本。
     */
    public TranslateAction(@NotNull AutoSelectionMode autoSelectionMode, boolean checkSelection) {
        super(Icons.Translate);
        mAutoSelectionMode = Utils.requireNonNull(autoSelectionMode, "selectionMode cannot be null.");
        mCheckSelection = checkSelection;
    }

    /**
     * 自动从最大范围内取词，忽略选择
     */
    public TranslateAction() {
        this(AutoSelectionMode.INCLUSIVE, false);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;

        final Editor editor = getEditor(e);
        if (editor != null && hasQueryTextRange()) {
            final TextRange queryTextRange = mQueryTextRange;

            //noinspection ConstantConditions
            final String queryText = Utils.splitWord(editor.getDocument().getText(queryTextRange));
            if (!Utils.isEmptyOrBlankString(queryText)) {
                final Project project = e.getProject();
                final ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
                final HighlightManager highlightManager = project == null ? null : HighlightManager.getInstance(project);

                if (highlightManager != null) {
                    highlightManager.addRangeHighlight(editor, queryTextRange.getStartOffset(),
                            queryTextRange.getEndOffset(), HIGHLIGHT_ATTRIBUTES, true, highlighters);
                }

                moveCaret(editor);

                TranslationBalloon translationBalloon = new TranslationBalloon(editor);
                translationBalloon.showAndQuery(queryText);

                if (!highlighters.isEmpty() && highlightManager != null) {
                    Disposer.register(translationBalloon.getDisposable(), new Disposable() {
                        @Override
                        public void dispose() {
                            for (RangeHighlighter highlighter : highlighters) {
                                highlightManager.removeSegmentHighlighter(editor, highlighter);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * 移动光标到目标字符串中间
     */
    private void moveCaret(Editor editor) {
        final TextRange queryTextRange = mQueryTextRange;

        final CaretModel caretModel = editor.getCaretModel();
        //noinspection ConstantConditions
        caretModel.moveToOffset(queryTextRange.getEndOffset());

        int lineNumber = caretModel.getLogicalPosition().line;
        Document document = editor.getDocument();
        if (lineNumber >= document.getLineCount()) {
            return;
        }

        int caretOffset = editor.getCaretModel().getOffset();
        int textLength = document.getTextLength();
        if (caretOffset == textLength) caretOffset--;
        if (caretOffset < 0) return;

        int line = document.getLineNumber(caretOffset);
        int lineStartOffset = document.getLineStartOffset(line);
        int queryStartOffset = queryTextRange.getStartOffset();
        int queryEndOffset = queryTextRange.getEndOffset();

        if (lineStartOffset <= queryStartOffset) {
            caretModel.moveToOffset(Math.round((queryStartOffset + queryEndOffset) / 2f));
        }
    }

    @Nullable
    private Editor getEditor(AnActionEvent e) {
        return CommonDataKeys.EDITOR.getData(e.getDataContext());
    }

    private boolean hasQueryTextRange() {
        return mQueryTextRange != null && !mQueryTextRange.isEmpty();
    }

    @Override
    public void update(AnActionEvent e) {
        mQueryTextRange = getQueryTextRange(e);
        e.getPresentation().setEnabledAndVisible(hasQueryTextRange());
    }

    @Nullable
    private TextRange getQueryTextRange(AnActionEvent e) {
        TextRange queryRange = null;

        Editor editor = getEditor(e);
        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            if (mCheckSelection && selectionModel.hasSelection()) {
                queryRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            } else {
                final ArrayList<TextRange> ranges = new ArrayList<TextRange>();
                final int offset = editor.getCaretModel().getOffset();
                final boolean exclusiveMode = mAutoSelectionMode == AutoSelectionMode.EXCLUSIVE;

                SelectWordUtilCompat.addWordOrLexemeSelection(exclusiveMode, editor, offset, ranges);

                if (!ranges.isEmpty()) {
                    if (exclusiveMode) {
                        queryRange = ranges.get(0);
                    } else {
                        TextRange maxRange = null;
                        for (TextRange range : ranges) {
                            if (maxRange == null || range.contains(maxRange)) {
                                maxRange = range;
                            }
                        }

                        queryRange = maxRange;
                    }
                }
            }
        }

        return queryRange;
    }

    /**
     * 取词模式
     */
    public enum AutoSelectionMode {
        /**
         * 只取一个词
         */
        EXCLUSIVE,
        /**
         * 最大范围内取词
         */
        INCLUSIVE
    }
}

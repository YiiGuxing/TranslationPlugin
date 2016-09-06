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
import com.sun.istack.internal.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditorPopupMenuAction extends AnAction implements DumbAware {

    private static final TextAttributes HIGHLIGHT_ATTRIBUTES;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(new JBColor(new Color(0xFFE4E4FF), new Color(0xFF344134)));
        attributes.setEffectType(EffectType.LINE_UNDERSCORE);
        attributes.setEffectColor(new JBColor(0xFFEE6000, 0xFFCC7832));

        HIGHLIGHT_ATTRIBUTES = attributes;
    }

    @Nullable
    private TextRange mQueryTextRange;

    public EditorPopupMenuAction() {
        super("Translate", "Translate", Icons.Translate);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;

        final Editor editor = getEditor(e);
        if (editor != null && hasQueryTextRange()) {
            final TextRange queryTextRange = mQueryTextRange;

            final String queryText = Utils.splitWord(editor.getDocument().getText(queryTextRange));
            if (!Utils.isEmptyOrBlankString(queryText)) {
                final SelectionModel selectionModel = editor.getSelectionModel();
                final Project project = e.getProject();

                final ArrayList<RangeHighlighter> highlighters = new ArrayList<>();
                final HighlightManager highlightManager = project == null ? null : HighlightManager.getInstance(project);
                if (!selectionModel.hasSelection() && highlightManager != null) {
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
            if (!selectionModel.hasSelection()) {
                final List<TextRange> ranges = new ArrayList<>();
                final int offset = editor.getCaretModel().getOffset();
                SelectWordUtilCompat.addWordOrLexemeSelection(false, editor, offset, ranges);

                if (!ranges.isEmpty()) {
                    TextRange maxRange = null;
                    for (TextRange range : ranges) {
                        if (maxRange == null || range.contains(maxRange)) {
                            maxRange = range;
                        }
                    }

                    queryRange = maxRange;
                }
            } else {
                queryRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            }
        }

        return queryRange;
    }

}

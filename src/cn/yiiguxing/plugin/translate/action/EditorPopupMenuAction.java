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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.sun.istack.internal.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EditorPopupMenuAction extends AnAction implements DumbAware {

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
            String queryText = Utils.splitWord(editor.getDocument().getText(mQueryTextRange));
            if (!Utils.isEmptyOrBlankString(queryText)) {
                SelectionModel selectionModel = editor.getSelectionModel();

                final ArrayList<RangeHighlighter> highlighters = new ArrayList<>();
                final HighlightManager highlightManager = HighlightManager.getInstance(e.getProject());
                if (!selectionModel.hasSelection()) {
                    EditorColorsManager colorsManager = EditorColorsManager.getInstance();
                    TextAttributes attributes = colorsManager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
                    highlightManager.addRangeHighlight(editor, mQueryTextRange.getStartOffset(), mQueryTextRange.getEndOffset(), attributes, true, highlighters);
                }

                TranslationBalloon translationBalloon = new TranslationBalloon(editor);
                translationBalloon.showAndQuery(queryText);

                System.out.println(highlighters.size());
                if (!highlighters.isEmpty()) {
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

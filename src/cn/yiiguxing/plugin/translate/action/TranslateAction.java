package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
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
import com.intellij.openapi.editor.RangeMarker;
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

    private final boolean mCheckSelection;

    @Nullable
    private TextRange mQueryTextRange;

    /**
     * @param checkSelection 指定是否检查手动选择的文本。<code>true</code> - 如果有手动选择文本，
     *                       则忽略<code>autoSelectionMode</code>, <code>false</code> - 将忽略手动选择的文本。
     */
    public TranslateAction(boolean checkSelection) {
        super(Icons.Translate);
        mCheckSelection = checkSelection;
    }

    /**
     * 自动从最大范围内取词，忽略选择
     */
    public TranslateAction() {
        this(false);
    }

    /**
     * 返回取词模式
     */
    @NotNull
    protected AutoSelectionMode getAutoSelectionMode() {
        return AutoSelectionMode.INCLUSIVE;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;

        final Editor editor = getEditor(e);
        if (editor != null && hasQueryTextRange()) {
            final TextRange queryTextRange = Utils.requireNonNull(mQueryTextRange);

            final String queryText = Utils.splitWord(editor.getDocument().getText(queryTextRange));
            if (!Utils.isEmptyOrBlankString(queryText)) {
                final Project project = e.getProject();
                final ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
                final HighlightManager highlightManager = project == null ? null : HighlightManager.getInstance(project);

                if (highlightManager != null) {
                    highlightManager.addRangeHighlight(editor, queryTextRange.getStartOffset(),
                            queryTextRange.getEndOffset(), HIGHLIGHT_ATTRIBUTES, true, highlighters);
                }

                RangeMarker caretRangeMarker = createCaretRangeMarker(editor);
                TranslationBalloon translationBalloon = TranslationUiManager.getInstance()
                        .showTranslationBalloon(editor, caretRangeMarker, queryText);

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

    @NotNull
    private RangeMarker createCaretRangeMarker(@NotNull Editor editor) {
        RangeMarker myCaretRangeMarker = editor.getDocument().createRangeMarker(Utils.requireNonNull(mQueryTextRange));
        myCaretRangeMarker.setGreedyToLeft(true);
        myCaretRangeMarker.setGreedyToRight(true);

        return myCaretRangeMarker;
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

                final AutoSelectionMode selectionMode = Utils.requireNonNull(getAutoSelectionMode(),
                        "Method getAutoSelectionMode() can not return null.");
                final boolean exclusiveMode = selectionMode == AutoSelectionMode.EXCLUSIVE;

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

}

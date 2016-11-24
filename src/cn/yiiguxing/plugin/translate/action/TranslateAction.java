package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.TranslationUiManager;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.ui.Icons;
import cn.yiiguxing.plugin.translate.ui.TranslationBalloon;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;

public class TranslateAction extends AutoSelectAction implements DumbAware {

    private static final TextAttributes HIGHLIGHT_ATTRIBUTES;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setBackgroundColor(new JBColor(new Color(0xFFE4E4FF), new Color(0xFF344134)));
        attributes.setEffectType(EffectType.LINE_UNDERSCORE);
        attributes.setEffectColor(new JBColor(0xFFEE6000, 0xFFCC7832));

        HIGHLIGHT_ATTRIBUTES = attributes;
    }

    /**
     * @param checkSelection 指定是否检查手动选择的文本。<code>true</code> - 如果有手动选择文本，
     *                       则忽略<code>autoSelectionMode</code>, <code>false</code> - 将忽略手动选择的文本。
     */
    public TranslateAction(boolean checkSelection) {
        super(Icons.Translate, checkSelection);
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
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return AutoSelectionMode.INCLUSIVE;
    }

    @Override
    protected void onUpdate(AnActionEvent e, boolean active) {
        e.getPresentation().setEnabledAndVisible(active);
    }

    @Override
    protected void onActionPerformed(AnActionEvent e, @NotNull final Editor editor, @NotNull TextRange selectionRange) {
        final String queryText = Utils.splitWord(editor.getDocument().getText(selectionRange));
        if (!Utils.isEmptyOrBlankString(queryText)) {
            final Project project = editor.getProject();
            final ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
            final HighlightManager highlightManager = project == null ? null : HighlightManager.getInstance(project);

            if (highlightManager != null) {
                highlightManager.addRangeHighlight(editor, selectionRange.getStartOffset(),
                        selectionRange.getEndOffset(), HIGHLIGHT_ATTRIBUTES, true, highlighters);
            }

            RangeMarker caretRangeMarker = createCaretRangeMarker(editor, selectionRange);
            TranslationBalloon translationBalloon = TranslationUiManager.getInstance()
                    .showTranslationBalloon(editor, caretRangeMarker, queryText);

            if (!highlighters.isEmpty() && highlightManager != null) {
                Disposer.register(translationBalloon.getDisposable(), new Disposable() {
                    @Override
                    public void dispose() {
                        for (RangeHighlighter highlighter : highlighters) {
                            highlighter.dispose();
                        }
                    }
                });
            }
        }
    }

    @NotNull
    private RangeMarker createCaretRangeMarker(@NotNull Editor editor, @NotNull TextRange selectionRange) {
        RangeMarker myCaretRangeMarker = editor.getDocument().createRangeMarker(Utils.requireNonNull(selectionRange));
        myCaretRangeMarker.setGreedyToLeft(true);
        myCaretRangeMarker.setGreedyToRight(true);

        return myCaretRangeMarker;
    }

}

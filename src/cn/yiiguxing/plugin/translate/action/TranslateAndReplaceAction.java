package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.Translator;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.compat.SelectWordUtilCompat;
import cn.yiiguxing.plugin.translate.model.BasicExplain;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 翻译并替换
 */
public class TranslateAndReplaceAction extends AutoSelectAction {

    private static final String PATTERN_FIX = "^(\\[[\\u4E00-\\u9FBF]+])+ ";

    private static final TextAttributes HIGHLIGHT_ATTRIBUTES;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setEffectType(EffectType.BOXED);
        attributes.setEffectColor(new JBColor(0xFFFF0000, 0xFFFF0000));

        HIGHLIGHT_ATTRIBUTES = attributes;
    }

    private final Settings mSettings;

    public TranslateAndReplaceAction() {
        super(SelectWordUtilCompat.HANZI_CONDITION, true);
        setEnabledInModalContext(false);
        mSettings = Settings.getInstance();
    }

    @NotNull
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return mSettings.getAutoSelectionMode();
    }

    @Override
    protected void onUpdate(AnActionEvent e, boolean active) {
        if (active) {
            final Editor editor = getEditor(e);
            if (editor != null) {
                SelectionModel selectionModel = editor.getSelectionModel();
                if (selectionModel.hasSelection()) {
                    final String selectedText = selectionModel.getSelectedText();
                    if (!Utils.isEmptyOrBlankString(selectedText)) {
                        boolean hasHanzi = false;
                        for (int i = 0; i < selectedText.length(); i++) {
                            if (SelectWordUtilCompat.HANZI_CONDITION.value(selectedText.charAt(i))) {
                                hasHanzi = true;
                                break;
                            }
                        }

                        active = hasHanzi;
                    }
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(active);
    }

    @Override
    protected void onActionPerformed(final AnActionEvent e,
                                     @NotNull final Editor editor,
                                     @NotNull final TextRange selectionRange) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getProject();
        if (project == null || (virtualFile != null &&
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile).hasReadonlyFiles())) {
            return;
        }

        final String text = editor.getDocument().getText(selectionRange);
        if (Utils.isEmptyOrBlankString(text))
            return;

        Translator.getInstance().query(text, new Translator.Callback() {
            @Override
            public void onQuery(@Nullable String query, @Nullable final QueryResult result) {
                if (result == null || !result.isSuccessful())
                    return;

                final List<LookupElement> replaceLookup = getReplaceLookupElements(result);
                if (replaceLookup.isEmpty())
                    return;

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        doReplace(editor, selectionRange, text, replaceLookup);
                    }
                });
            }
        });
    }

    private static void doReplace(@NotNull final Editor editor,
                                  @NotNull final TextRange selectionRange,
                                  @NotNull final String targetText,
                                  @NotNull final List<LookupElement> replaceLookup) {
        if (editor.isDisposed() || editor.getProject() == null ||
                !targetText.equals(editor.getDocument().getText(selectionRange)) ||
                !selectionRange.containsOffset(editor.getCaretModel().getOffset())) {
            return;
        }

        final SelectionModel selectionModel = editor.getSelectionModel();
        final int startOffset = selectionRange.getStartOffset();
        final int endOffset = selectionRange.getEndOffset();
        if (selectionModel.hasSelection()) {
            if (selectionModel.getSelectionStart() != startOffset || selectionModel.getSelectionEnd() != endOffset) {
                return;
            }
        } else {
            selectionModel.setSelection(startOffset, endOffset);
        }

        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        editor.getCaretModel().moveToOffset(endOffset);

        LookupElement[] items = replaceLookup.toArray(new LookupElement[replaceLookup.size()]);
        final LookupEx lookup = LookupManager.getInstance(editor.getProject()).showLookup(editor, items);

        if (lookup == null) {
            return;
        }

        final HighlightManager highlightManager = HighlightManager.getInstance(editor.getProject());
        final List<RangeHighlighter> highlighters = addHighlight(highlightManager, editor, selectionRange);

        lookup.addLookupListener(new LookupAdapter() {
            @Override
            public void itemSelected(LookupEvent event) {
                disposeHighlight(highlighters);
            }

            @Override
            public void lookupCanceled(LookupEvent event) {
                selectionModel.removeSelection();
                disposeHighlight(highlighters);
            }
        });
    }

    @NotNull
    private static List<RangeHighlighter> addHighlight(@NotNull HighlightManager highlightManager,
                                                       @NotNull Editor editor,
                                                       @NotNull TextRange selectionRange) {
        final ArrayList<RangeHighlighter> highlighters = new ArrayList<RangeHighlighter>();
        highlightManager.addOccurrenceHighlight(editor, selectionRange.getStartOffset(), selectionRange.getEndOffset(),
                HIGHLIGHT_ATTRIBUTES, 0, highlighters, null);

        for (RangeHighlighter highlighter : highlighters) {
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
        }

        return highlighters;
    }

    private static void disposeHighlight(@NotNull List<RangeHighlighter> highlighters) {
        for (RangeHighlighter highlighter : highlighters) {
            highlighter.dispose();
        }
    }

    @NotNull
    private static List<LookupElement> getReplaceLookupElements(@NotNull QueryResult result) {
        final List<LookupElement> replaceLookup;

        BasicExplain basicExplain = result.getBasicExplain();
        if (basicExplain != null) {
            replaceLookup = getReplaceLookupElements(Utils.expandExplain(basicExplain.getExplains()));
        } else {
            replaceLookup = getReplaceLookupElements(result.getTranslation());
        }

        return replaceLookup;
    }

    @NotNull
    private static List<LookupElement> getReplaceLookupElements(@Nullable String[] explains) {
        if (explains == null || explains.length == 0)
            return Collections.emptyList();

        final Set<LookupElement> camel = new LinkedHashSet<LookupElement>();
        final Set<LookupElement> pascal = new LinkedHashSet<LookupElement>();
        final Set<LookupElement> lowerWithUnder = new LinkedHashSet<LookupElement>();
        final Set<LookupElement> capsWithUnder = new LinkedHashSet<LookupElement>();
        final Set<LookupElement> withSpace = new LinkedHashSet<LookupElement>();

        final StringBuilder camelBuilder = new StringBuilder();
        final StringBuilder pascalBuilder = new StringBuilder();
        final StringBuilder lowerWithUnderBuilder = new StringBuilder();
        final StringBuilder capsWithUnderBuilder = new StringBuilder();
        final StringBuilder withSpaceBuilder = new StringBuilder();

        for (String explain : explains) {
            List<String> words = fixAndSplitForVariable(explain);
            if (words == null || words.isEmpty()) {
                continue;
            }

            camelBuilder.setLength(0);
            pascalBuilder.setLength(0);
            lowerWithUnderBuilder.setLength(0);
            capsWithUnderBuilder.setLength(0);
            withSpaceBuilder.setLength(0);

            build(words, camelBuilder, pascalBuilder, lowerWithUnderBuilder, capsWithUnderBuilder, withSpaceBuilder);

            camel.add(LookupElementBuilder.create(camelBuilder.toString()));
            pascal.add(LookupElementBuilder.create(pascalBuilder.toString()));
            lowerWithUnder.add(LookupElementBuilder.create(lowerWithUnderBuilder.toString()));
            capsWithUnder.add(LookupElementBuilder.create(capsWithUnderBuilder.toString()));
            withSpace.add(LookupElementBuilder.create(withSpaceBuilder.toString()));
        }

        final Set<LookupElement> result = new LinkedHashSet<LookupElement>();
        result.addAll(camel);
        result.addAll(pascal);
        result.addAll(lowerWithUnder);
        result.addAll(capsWithUnder);
        result.addAll(withSpace);

        return Collections.unmodifiableList(new ArrayList<LookupElement>(result));
    }

    private static void build(@NotNull final List<String> words,
                              @NotNull final StringBuilder camel,
                              @NotNull final StringBuilder pascal,
                              @NotNull final StringBuilder lowerWithUnder,
                              @NotNull final StringBuilder capsWithUnder,
                              @NotNull final StringBuilder withSpace) {
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);

            if (i > 0) {
                lowerWithUnder.append('_');
                capsWithUnder.append('_');
                withSpace.append(' ');
            }

            withSpace.append(word);

            if (i == 0) {
                word = sanitizeJavaIdentifierStart(word);
            }

            String capitalized = StringUtil.capitalizeWithJavaBeanConvention(word);
            String lowerCase = word.toLowerCase();

            camel.append(i == 0 ? lowerCase : capitalized);
            pascal.append(capitalized);
            lowerWithUnder.append(lowerCase);
            capsWithUnder.append(word.toUpperCase());
        }
    }

    @Nullable
    private static List<String> fixAndSplitForVariable(@NotNull String explains) {
        String explain = Utils.splitExplain(explains)[1];
        if (Utils.isEmptyOrBlankString(explain)) {
            return null;
        }

        String fixed = explain.replaceFirst(PATTERN_FIX, "");
        return StringUtil.getWordsIn(fixed);
    }

    private static String sanitizeJavaIdentifierStart(@NotNull String name) {
        return Character.isJavaIdentifierStart(name.charAt(0)) ? name : "_" + name;
    }

}

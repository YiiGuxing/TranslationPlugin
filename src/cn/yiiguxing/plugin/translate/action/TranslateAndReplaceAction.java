package cn.yiiguxing.plugin.translate.action;

import cn.yiiguxing.plugin.translate.Settings;
import cn.yiiguxing.plugin.translate.Translator;
import cn.yiiguxing.plugin.translate.Utils;
import cn.yiiguxing.plugin.translate.model.BasicExplain;
import cn.yiiguxing.plugin.translate.model.QueryResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 翻译并替换
 */
public class TranslateAndReplaceAction extends AutoSelectAction {

    private static final Pattern PATTERN_CHINESE = Pattern.compile("[\\u4E00-\\u9FBF]");
    private static final String PATTERN_FIX = "^\\[[\\u4E00-\\u9FBF]+\\] ";

    private static final TextAttributes HIGHLIGHT_ATTRIBUTES;
    private static final String GROUP_ID = "TranslateAndReplaceAction.GROUP_ID";

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setEffectType(EffectType.BOXED);
        attributes.setEffectColor(new JBColor(0xFFFF0000, 0xFFFF0000));

        HIGHLIGHT_ATTRIBUTES = attributes;
    }

    private final Settings settings;

    public TranslateAndReplaceAction() {
        super(true);
        settings = Settings.getInstance();
    }

    @NotNull
    @Override
    protected AutoSelectionMode getAutoSelectionMode() {
        return settings.getAutoSelectionMode();
    }

    @Override
    protected void onUpdate(AnActionEvent e, boolean active) {
        e.getPresentation().setEnabledAndVisible(active);
    }

    @Override
    protected void onActionPerformed(final AnActionEvent e, @NotNull final Editor editor, @NotNull final TextRange selectionRange) {
        VirtualFile virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        final Project project = e.getProject();
        if (project == null || (virtualFile != null &&
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFile).hasReadonlyFiles())) {
            return;
        }

        final String text = editor.getDocument().getText(selectionRange);
        if (!PATTERN_CHINESE.matcher(text).find()) {
            return;
        }

        final String queryText = Utils.splitWord(text);
        if (Utils.isEmptyOrBlankString(queryText))
            return;

        Translator.getInstance().query(queryText, new Translator.Callback() {
            @Override
            public void onQuery(@Nullable String query, @Nullable final QueryResult result) {
                if (result == null || result.getErrorCode() != QueryResult.ERROR_CODE_NONE)
                    return;

                final List<List<String>> replaceData = getReplaceData(result);
                if (replaceData.isEmpty())
                    return;

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        doReplace(editor, selectionRange, text, replaceData);
                    }
                });
            }
        });
    }

    private void doReplace(@NotNull final Editor editor,
                           @NotNull final TextRange selectionRange,
                           @NotNull final String targetText,
                           @NotNull final List<List<String>> replaceData) {
        if (editor.isDisposed())
            return;

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (editor.isDisposed() || !targetText.equals(editor.getDocument().getText(selectionRange)) ||
                        !selectionRange.containsOffset(editor.getCaretModel().getOffset()))
                    return;

                final Runnable command = new Runnable() {
                    @Override
                    public void run() {
                        int start = selectionRange.getStartOffset();
                        int end = selectionRange.getEndOffset();
                        String replace = replaceData.get(0).get(0);
                        editor.getDocument().replaceString(start, end, replace);
                    }
                };

                CommandProcessor.getInstance().executeCommand(editor.getProject(), command,
                        getTemplatePresentation().getText(),
                        DocCommandGroupId.withGroupId(editor.getDocument(), GROUP_ID),
                        UndoConfirmationPolicy.DEFAULT,
                        editor.getDocument());
            }
        });
    }

    @NotNull
    private static List<List<String>> getReplaceData(@NotNull QueryResult result) {
        final List<List<String>> replaceData;

        BasicExplain basicExplain = result.getBasicExplain();
        if (basicExplain != null && basicExplain.getExplains().length == 1) {
            replaceData = getMixedReplaceData(basicExplain.getExplains()[0]);
        } else if (basicExplain == null && result.getTranslation().length == 1) {
            replaceData = getMixedReplaceData(result.getTranslation()[0]);
        } else if (basicExplain != null) {
            replaceData = getLooseReplaceData(basicExplain.getExplains());
        } else {
            replaceData = getLooseReplaceData(result.getTranslation());
        }

        return replaceData;
    }

    @NotNull
    private static List<List<String>> getMixedReplaceData(@NotNull String explain) {
        final List<String> words = fixAndSplitForVariable(explain);
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        final StringBuilder camelBuilder = new StringBuilder();
        final StringBuilder pascalBuilder = new StringBuilder();
        final StringBuilder lowerWithUnderBuilder = new StringBuilder();
        final StringBuilder capsWithUnderBuilder = new StringBuilder();
        final StringBuilder withSpaceBuilder = new StringBuilder();

        build(words, camelBuilder, pascalBuilder, lowerWithUnderBuilder, capsWithUnderBuilder, withSpaceBuilder);

        LinkedHashSet<String> set = new LinkedHashSet<String>();
        set.add(camelBuilder.toString());
        set.add(pascalBuilder.toString());
        set.add(lowerWithUnderBuilder.toString());
        set.add(capsWithUnderBuilder.toString());
        set.add(withSpaceBuilder.toString());

        return Collections.singletonList((List<String>) new ArrayList<String>(set));
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

    @NotNull
    private static List<List<String>> getLooseReplaceData(@NotNull String[] explains) {
        final StringBuilder camelBuilder = new StringBuilder();
        final StringBuilder pascalBuilder = new StringBuilder();
        final StringBuilder lowerWithUnderBuilder = new StringBuilder();
        final StringBuilder capsWithUnderBuilder = new StringBuilder();
        final StringBuilder withSpaceBuilder = new StringBuilder();

        final List<String> camel = new SmartList<String>();
        final List<String> pascal = new SmartList<String>();
        final List<String> lowerWithUnder = new SmartList<String>();
        final List<String> capsWithUnder = new SmartList<String>();
        final List<String> withSpace = new SmartList<String>();

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

            camel.add(camelBuilder.toString());
            pascal.add(pascalBuilder.toString());
            lowerWithUnder.add(lowerWithUnderBuilder.toString());
            capsWithUnder.add(capsWithUnderBuilder.toString());
            withSpace.add(withSpaceBuilder.toString());
        }

        LinkedHashSet<List<String>> resultSet = new LinkedHashSet<List<String>>();
        if (!camel.isEmpty())
            resultSet.add(camel);
        if (!pascal.isEmpty())
            resultSet.add(pascal);
        if (!lowerWithUnder.isEmpty())
            resultSet.add(lowerWithUnder);
        if (!capsWithUnder.isEmpty())
            resultSet.add(capsWithUnder);
        if (!withSpace.isEmpty())
            resultSet.add(withSpace);

        return new ArrayList<List<String>>(resultSet);
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

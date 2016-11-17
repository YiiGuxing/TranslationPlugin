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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * 翻译并替换
 */
public class TranslateAndReplaceAction extends AutoSelectAction {

    private static final Pattern PATTERN_CHINESE = Pattern.compile("[\\u4E00-\\u9FBF]");
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

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        if (editor.isDisposed())
                            return;

                        final Runnable command = new Runnable() {
                            @Override
                            public void run() {
                                Document document = editor.getDocument();
                                if (text.equals(document.getText(selectionRange))) {
                                    int start = selectionRange.getStartOffset();
                                    int end = selectionRange.getEndOffset();
                                    String replace = null;
                                    BasicExplain basicExplain = result.getBasicExplain();
                                    if (basicExplain != null) {
                                        String[] explains = basicExplain.getExplains();
                                        if (explains != null && explains.length > 0) {
                                            replace = explains[0];
                                        }
                                    } else {
                                        String[] translation = result.getTranslation();
                                        if (translation != null && translation.length > 0) {
                                            replace = translation[0];
                                        }
                                    }

                                    if (replace != null && !Utils.isEmptyOrBlankString(replace)) {
                                        replace = replace.trim().toLowerCase().replaceAll("[\\p{Punct}\\p{P}\\s￥]+", "_");
                                        document.replaceString(start, end, replace);
                                    }
                                }
                            }
                        };

                        CommandProcessor.getInstance().executeCommand(project, command,
                                getTemplatePresentation().getText(),
                                DocCommandGroupId.withGroupId(editor.getDocument(), GROUP_ID),
                                UndoConfirmationPolicy.DEFAULT,
                                editor.getDocument());
                    }
                });
            }
        });
    }
}

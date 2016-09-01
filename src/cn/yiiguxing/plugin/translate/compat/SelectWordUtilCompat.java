package cn.yiiguxing.plugin.translate.compat;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("SpellCheckingInspection")
public final class SelectWordUtilCompat {

    private SelectWordUtilCompat() {
    }

    public static void addWordOrLexemeSelection(boolean camel,
                                                @NotNull Editor editor,
                                                int cursorOffset,
                                                @NotNull List<TextRange> ranges) {
        if (IdeaCompat.BUILD_NUMBER >= IdeaCompat.Version.IDEA2016_2) {
            SelectWordUtil.addWordOrLexemeSelection(camel, editor, cursorOffset, ranges);
        } else {
            CharSequence editorText = editor.getDocument().getImmutableCharSequence();
            SelectWordUtil.addWordSelection(camel, editorText, cursorOffset, ranges);
        }

    }

}

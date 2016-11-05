package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.ui.TranslationBalloon;
import cn.yiiguxing.plugin.translate.ui.TranslationDialog;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class TranslationUiManager {

    private TranslationBalloon myShowingBalloon;
    private TranslationDialog myShowingDialog;

    private TranslationUiManager() {
    }

    public static TranslationUiManager getInstance() {
        return ServiceManager.getService(TranslationUiManager.class);
    }

    public TranslationBalloon showTranslationBalloon(@NotNull Editor editor,
                                                     @NotNull RangeMarker caretRangeMarker,
                                                     @NotNull String queryText) {
        if (myShowingBalloon != null) {
            myShowingBalloon.hide();
        }

        myShowingBalloon = new TranslationBalloon(editor, caretRangeMarker);
        Disposer.register(myShowingBalloon.getDisposable(), new Disposable() {
            @Override
            public void dispose() {
                myShowingBalloon = null;
            }
        });
        myShowingBalloon.showAndQuery(queryText);

        return myShowingBalloon;
    }

    /**
     * 显示对话框
     */
    @NotNull
    public TranslationDialog showTranslationDialog(@Nullable Project project) {
        if (myShowingDialog == null) {
            myShowingDialog = new TranslationDialog(project);
            Disposer.register(myShowingDialog.getDisposable(), new Disposable() {
                @Override
                public void dispose() {
                    myShowingDialog = null;
                }
            });
        }

        myShowingDialog.show();
        return myShowingDialog;
    }

    /**
     * 测试是否有正在显示的对话框
     */
    public boolean hasTranslationDialogShowing() {
        return myShowingDialog != null && myShowingDialog.isShowing();
    }

    /**
     * 更新当前显示的对话框
     */
    public void updateCurrentShowingTranslationDialog() {
        if (hasTranslationDialogShowing()) {
            myShowingDialog.update();
        }
    }

    /**
     * 如果有对话框正在显示，则返回当前显示的对话框，否则返回null.
     */
    @Nullable
    public TranslationDialog getCurrentShowingDialog() {
        if (!hasTranslationDialogShowing()) {
            return myShowingDialog;
        }

        return null;
    }

    /**
     * 通知历史记录变化
     */
    public void notifyHistoriesChanged() {
        if (hasTranslationDialogShowing()) {
            myShowingDialog.updateHistory(false);
        }
    }

}

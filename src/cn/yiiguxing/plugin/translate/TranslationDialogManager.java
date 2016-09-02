package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.ui.TranslationDialog;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class TranslationDialogManager {

    private TranslationDialog myShowingDialog;

    private TranslationDialogManager() {
    }

    public static TranslationDialogManager getInstance() {
        return ServiceManager.getService(TranslationDialogManager.class);
    }

    /**
     * 显示对话框
     */
    public void showTranslationDialog(@Nullable Project project) {
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
    }

    /**
     * 测试是否有正在显示的对话框
     */
    public boolean hasShowing() {
        return myShowingDialog != null && myShowingDialog.isShowing();
    }

    /**
     * 更新当前显示的对话框
     */
    public void updateCurrentShowingTranslationDialog() {
        if (hasShowing()) {
            myShowingDialog.update();
        }
    }

    /**
     * 如果有对话框正在显示，则返回当前显示的对话框，否则返回null.
     */
    @Nullable
    public TranslationDialog getCurrentShowingDialog() {
        if (!hasShowing()) {
            return myShowingDialog;
        }

        return null;
    }

    /**
     * 通知历史记录变化
     */
    public void notifyHistoriesChanged() {
        if (hasShowing()) {
            myShowingDialog.updateHistory(false);
        }
    }

}

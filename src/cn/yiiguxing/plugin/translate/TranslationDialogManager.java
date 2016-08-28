package cn.yiiguxing.plugin.translate;

import cn.yiiguxing.plugin.translate.ui.TranslationDialog;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
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
            myShowingDialog.setOnDisposeListener(new TranslationDialog.OnDisposeListener() {
                @Override
                public void onDispose() {
                    myShowingDialog = null;
                }
            });
        }

        myShowingDialog.show();
    }

    /**
     * 更新当前显示的对话框
     */
    public void updateCurrentShowingTranslationDialog() {
        if (myShowingDialog != null) {
            myShowingDialog.update();
        }
    }

    /**
     * 如果有对话框正在显示，则返回当前显示的对话框，否则返回null.
     */
    @Nullable
    public TranslationDialog getCurrentShowingDialog() {
        if (myShowingDialog != null && !myShowingDialog.isShowing()) {
            myShowingDialog = null;
        }

        return myShowingDialog;
    }

}

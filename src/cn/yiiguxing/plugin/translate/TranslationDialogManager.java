package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("WeakerAccess")
public class TranslationDialogManager {

    private final static TranslationDialogManager sInstance = new TranslationDialogManager();

    private TranslationDialog myShowingDialog;

    private TranslationDialogManager() {
    }

    public static TranslationDialogManager getInstance() {
        return sInstance;
    }

    public void show(@Nullable Project project) {
        if (myShowingDialog == null) {
            myShowingDialog = new TranslationDialog(project);
            myShowingDialog.getWindow().addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    myShowingDialog = null;
                }
            });
        }

        myShowingDialog.show();
    }

}

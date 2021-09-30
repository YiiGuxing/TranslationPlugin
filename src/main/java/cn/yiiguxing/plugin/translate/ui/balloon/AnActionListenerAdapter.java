package cn.yiiguxing.plugin.translate.ui.balloon;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import org.jetbrains.annotations.NotNull;

/**
 * The adapter for {@link AnActionListener} interface, {@link AnActionListener#beforeActionPerformed(AnAction, DataContext, AnActionEvent)}
 * will be removed in the future.
 */
interface AnActionListenerAdapter extends AnActionListener {

    // The implementation class must implement this method
    void beforeActionPerformed(@NotNull AnAction action, @NotNull AnActionEvent event);

    default void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        beforeActionPerformed(action, event);
    }

}

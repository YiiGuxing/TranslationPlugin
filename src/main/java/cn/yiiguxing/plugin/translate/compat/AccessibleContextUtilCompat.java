package cn.yiiguxing.plugin.translate.compat;

import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.swing.*;
import java.awt.*;

@SuppressWarnings("SpellCheckingInspection")
public final class AccessibleContextUtilCompat {

    private AccessibleContextUtilCompat() {
    }

    public static void setParent(@NotNull JComponent component, @Nullable Component newParent) {
        if (IdeaCompat.BUILD_NUMBER >= IdeaCompat.Version.IDEA2016_1) {
            AccessibleContextUtil.setParent(component, newParent);
        } else {
            setParent15(component, newParent);
        }
    }

    private static void setParent15(@NotNull JComponent component, @Nullable Component newParent) {
        if (newParent instanceof Accessible) {
            component.getAccessibleContext().setAccessibleParent((Accessible) newParent);
            return;
        }
        component.getAccessibleContext().setAccessibleParent(null);
    }

}

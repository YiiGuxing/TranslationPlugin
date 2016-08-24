package cn.yiiguxing.plugin.translate.compat;

import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            AccessibleContextUtilCompat15.setParent(component, newParent);
        }
    }

}

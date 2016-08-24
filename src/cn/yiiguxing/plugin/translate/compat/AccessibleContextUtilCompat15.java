package cn.yiiguxing.plugin.translate.compat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.swing.*;
import java.awt.*;

@SuppressWarnings("SpellCheckingInspection")
class AccessibleContextUtilCompat15 {

    static void setParent(@NotNull JComponent component, @Nullable Component newParent) {
        if (newParent instanceof Accessible) {
            component.getAccessibleContext().setAccessibleParent((Accessible) newParent);
            return;
        }
        component.getAccessibleContext().setAccessibleParent(null);
    }

}

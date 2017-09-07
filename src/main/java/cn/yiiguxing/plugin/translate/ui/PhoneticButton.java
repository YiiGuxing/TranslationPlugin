package cn.yiiguxing.plugin.translate.ui;

import com.intellij.util.Consumer;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Phonetic Button.
 */
public class PhoneticButton extends IconButton {

    private static final int MARGIN_LEFT = JBUI.scale(2);
    private static final int MARGIN_RIGHT = JBUI.scale(10);

    public PhoneticButton(@NotNull Consumer<MouseEvent> listener) {
        super(Icons.Speech, Icons.SpeechPressed, listener);

        setMaximumSize(new Dimension(Icons.Speech.getIconWidth() + MARGIN_LEFT + MARGIN_RIGHT,
                Icons.Speech.getIconHeight()));
        setAlignmentY(.84f);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, Icons.Speech.getIconWidth() + MARGIN_LEFT, height);
    }

    @Override
    public void setBounds(Rectangle r) {
        super.setBounds(new Rectangle(r.x, r.y, Icons.Speech.getIconWidth() + MARGIN_LEFT, r.height));
    }

    @Override
    protected void paintIcon(@NotNull Graphics g, @NotNull Icon icon) {
        icon.paintIcon(this, g, MARGIN_LEFT, (getHeight() - icon.getIconHeight()) / 2);
    }
}
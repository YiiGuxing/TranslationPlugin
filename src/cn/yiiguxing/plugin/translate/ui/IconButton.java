package cn.yiiguxing.plugin.translate.ui;

import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Icon Button.
 */
public class IconButton extends NonOpaquePanel {

    private final Icon icon;
    private final Icon pressIcon;
    private boolean isPressedByMouse;
    private boolean isActive = true;

    private final Consumer<MouseEvent> listener;

    public IconButton(@NotNull Icon icon, @Nullable Icon pressIcon, @NotNull Consumer<MouseEvent> listener) {
        this.icon = icon;
        this.pressIcon = pressIcon;
        this.listener = listener;

        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IconButton.this.listener.consume(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressedByMouse = true;
                IconButton.this.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressedByMouse = false;
                IconButton.this.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isPressedByMouse = false;
                IconButton.this.repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(icon.getIconWidth(), icon.getIconHeight());
    }

    public void setActive(final boolean active) {
        this.isActive = active;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hasPaint()) {
            paintIcon(g, (!isActive || isPressedByMouse) && pressIcon != null ? pressIcon : icon);
        }
    }

    protected boolean hasPaint() {
        return getWidth() > 0;
    }

    private void paintIcon(@NotNull Graphics g, @NotNull Icon icon) {
        icon.paintIcon(this, g, 0, (getHeight() - icon.getIconHeight()) / 2);
    }

}

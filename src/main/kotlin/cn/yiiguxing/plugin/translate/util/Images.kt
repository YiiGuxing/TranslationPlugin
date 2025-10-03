package cn.yiiguxing.plugin.translate.util

import com.intellij.util.ui.ImageUtil
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

fun Icon.toImage(): Image? {
    if (this is ImageIcon) {
        return image
    }

    val image = ImageUtil.createImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB)
    image.graphics.use {
        paintIcon(null, it, 0, 0)
    }

    return image
}

inline fun <G : Graphics> G.use(handler: (g: G) -> Unit) {
    try {
        handler(this)
    } finally {
        dispose()
    }
}
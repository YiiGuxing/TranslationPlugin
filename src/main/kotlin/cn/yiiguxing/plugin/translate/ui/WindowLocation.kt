package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.message

enum class WindowLocation(val displayName: String) {
    DEFAULT(message("window.location.default")),
    MOUSE_SCREEN(message("window.location.mouse.screen")),
    LAST_LOCATION(message("window.location.last.location"));
}
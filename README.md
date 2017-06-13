
TranslationPlugin [![GitHub release][release-img]][latest-release] [![Jetbrains Plugins][plugin-img]][plugin]
=================

[![Version](http://phpstorm.espend.de/badge/8579/version)][plugin]
[![Downloads](http://phpstorm.espend.de/badge/8579/downloads)][plugin]
[![Downloads last month](http://phpstorm.espend.de/badge/8579/last-month)][plugin]

### JetBrains IDE/Android Studio 翻译插件，支持中英互译、单词朗读。

![screenshots](./images/screenshots.gif)

![截图1](./images/balloon.png)

安装
----

兼容除 MPS 外所有产品编译号为143的 Jetbrains IDE 产品。

支持的 IDE:
- Android Studio
- IntelliJ IDEA
- IntelliJ IDEA Community Edition
- PhpStorm
- WebStorm
- PyCharm
- PyCharm Community Edition
- RubyMine
- AppCode
- CLion
- DataGrip
- Rider

**使用 IDE 内置插件系统:**
- <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>搜索并找到"Translation"</kbd> > <kbd>Install Plugin</kbd>

**手动:**
- 下载[`最新发布的插件包`][latest-release] -> <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Install plugin from disk...</kbd>

重启**IDE**.


Actions
-------

- **Show Translation Dialog...:** 打开翻译对话框。默认显示在工具栏上，默认快捷键为<kbd>Alt + 0</kbd>

  ![Translate](./images/action0.gif)

- **Translate:** 取词并翻译。如果有已选择的文本，优先从选择的文本内取词，否则默认以最大范围自动取词（该取词模式可在Settings中配置）。默认显示在编辑器右键菜单上，默认快捷键为<kbd>Alt + 1</kbd>

  ![Translate](./images/action1.gif)

- **Translate(Inclusive):** 取词并翻译。自动以最大范围取最近的所有词，忽略手动选择的文本。默认快捷键为<kbd>Alt + 2</kbd>

  ![Translate(Inclusive)](./images/action2.gif)

- **Translate(Exclusive):** 取词并翻译。自动取最近的单个词，忽略手动选择的文本。默认快捷键为<kbd>Alt + 3</kbd>

  ![Translate(Exclusive)](./images/action3.gif)

- **Translate And Replace...:** 翻译并替换。取词方式同`Translate`操作。默认显示在编辑器右键菜单上，默认快捷键为<kbd>Alt + R</kbd>

  ![Translate(Exclusive)](./images/replace.gif)

- **Translate Text Component:** 翻译一些文本组件（如快速文档、提示气泡、输入框……）中选中的文本。默认快捷键为<kbd>Alt + T</kbd>

  ![Translate(Exclusive)](./images/text_component.gif)
  

更新日志
--------

- 翻译API升级
- BUG修复

[release-img]: https://img.shields.io/github/release/YiiGuxing/TranslationPlugin.svg
[latest-release]: https://github.com/YiiGuxing/TranslationPlugin/releases/latest
[plugin-img]: https://img.shields.io/badge/plugin-8579-orange.svg
[plugin]: https://plugins.jetbrains.com/plugin/8579

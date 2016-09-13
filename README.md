Translation [![Gitter][badge-gitter-img]][badge-gitter] [![Jetbrains Plugins][plugin-img]][plugin] [![GitHub release][release-img]][latest-release]
===========

### Android Studio/IntelliJ IDEA 翻译插件,可中英互译。

![screenshots](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/screenshots.gif)

![截图1](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/0.png)

![截图2](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/1.png)


安装
----

- 使用**IDEA**内置插件系统:
  - <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>搜索并找到"Translation"</kbd> > <kbd>Install Plugin</kbd>
- 手动:
  - 下载[`最新发布的插件包`][latest-release] -> <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Install plugin from disk...</kbd>

重启**IDEA**.


使用
----

1. **打开翻译对话框:**

   点击工具栏上的 ![图标](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/2.png) 图标即可打开翻译对话框。

2. **翻译编辑器中选择的文本**:

   在编辑器中 <kbd>选择文本</kbd> > <kbd>单击鼠标右键</kbd> > <kbd>Translate</kbd>

   ![截图3](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/3.png)

3. **设置快捷键:**

   <kbd>Preferences(Settings)</kbd> > <kbd>Keymap</kbd> > <kbd>搜索Translate</kbd>。 搜索结果会有两个，Translate和TranslationDialog，Translate会取当前编辑器中选择的文本进行翻译（截图1），而TranslationDialog只会打开翻译对话框，不会取词。最后在需要添加快捷键项上 <kbd>右键</kbd> > <kbd>add Keyboard Shortcut...</kbd> 设置快捷键（默认无快捷键，按<kbd>ESC</kbd>键可关闭翻译对话框）。

4. **设置有道API KEY:**

   <kbd>Preferences(Settings)</kbd> > <kbd>Other Settings</kbd> > <kbd>Translation</kbd>。 由于有道的API在查询请求数量上存在限制，如果在1小时内查询请求次数达到一定数量后将会暂时禁止查询一段时间（大概1小时）。如果很多人同时使用同一个KEY，可能会很容易就达到了限制条件，这就可以通过使用自己的KEY来避免（一人一个KEY基本足够用了）。

   ![截图4](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/4.png)

5. **如何获取有道API KEY？**

   <kbd>Preferences(Settings)</kbd> > <kbd>Other Settings</kbd> > <kbd>Translation</kbd> > <kbd>获取有道API KEY</kbd>

   或者 [**点击这里**](http://fanyi.youdao.com/openapi?path=data-mode) 也可申请有道API KEY。

更新日志
--------

## [v1.2.1](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.2.1)(2016-08-29)

- 添加默认主题下的色彩样式
- 优化
  - 在翻译话框显示的情况下，同步翻译结果和历史记录。
  - 历史记录优化——鼠标在翻译文本过长而无法完全展示内容的历史记录条上悬停时，显示全部内容。
- Bug修复
  - 在非聚焦状态下按ESC键无法关闭对话框。
  - 翻译话框无法显示错误信息。
  - 其他Bug。

[完整的更新历史记录](./CHANGELOG.md)

[release-img]:        https://img.shields.io/github/release/YiiGuxing/TranslationPlugin.svg
[latest-release]:     https://github.com/YiiGuxing/TranslationPlugin/releases/latest
[badge-gitter-img]:   https://img.shields.io/gitter/room/YiiGuxing/TranslationPlugin.svg
[badge-gitter]:       https://gitter.im/TranslationPlugin/Lobby
[plugin-img]:         https://img.shields.io/badge/plugin-8579-green.svg
[plugin]:             https://plugins.jetbrains.com/plugin/8579

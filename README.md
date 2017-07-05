
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

兼容除 MPS 外所有产品编译号为143以上的 Jetbrains IDE 产品。

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


使用
----

1. **申请有道智云翻译服务:**
   - 注册[有道智云](http://ai.youdao.com)帐号并登录到控制台页面
   - 创建一个翻译实例：<kbd>控制台</kbd> > <kbd>自然语言翻译</kbd> > <kbd>翻译实例</kbd> > <kbd>创建翻译实例</kbd>
   - 创建一个应用并绑定翻译服务：<kbd>控制台</kbd> > <kbd>应用管理</kbd> > <kbd>我的应用</kbd> > <kbd>创建应用</kbd> > <kbd>绑定服务</kbd>
   - 在插件中绑定应用——将上步骤的**应用ID**和**应用密钥**填写到插件设置页中相应的位置：<kbd>Preferences(Settings)</kbd> > <kbd>\[Other Settings]</kbd> > <kbd>Translation</kbd> > <kbd>有道翻译</kbd>
   
   注：请注意保管好你的**应用密钥**，防止其泄漏。如帐号欠费，将无法使用。

2. **翻译编辑器中的文本:**

   在编辑器中 <kbd>选择文本或者鼠标指向文本</kbd> > <kbd>单击鼠标右键</kbd> > <kbd>Translate</kbd>

   ![翻译](./images/editor_popup_menu.png)

   或者使用快捷键<kbd>Alt + 1/2/3/T/R</kbd>进行翻译（Mac下默认快捷键可能无效，需要自定义快捷键），详见 **[Actions](#actions)**

3. **自定义快捷键（Mac下默认快捷键可能无效）:**

   <kbd>Preferences(Settings)</kbd> > <kbd>Keymap</kbd> > <kbd>搜索Translation</kbd>。在需要添加快捷键Action上 <kbd>右键</kbd> > <kbd>add Keyboard Shortcut...</kbd> 设置快捷键（按<kbd>ESC</kbd>键可关闭气泡和翻译对话框）。

   ![keymap](./images/keymap.png)
   

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

## [v1.3.6](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.3.6) (2017-07-05)

- Bug修复

[完整的更新历史记录](./CHANGELOG.md)

[release-img]: https://img.shields.io/github/release/YiiGuxing/TranslationPlugin.svg
[latest-release]: https://github.com/YiiGuxing/TranslationPlugin/releases/latest
[plugin-img]: https://img.shields.io/badge/plugin-8579-orange.svg
[plugin]: https://plugins.jetbrains.com/plugin/8579

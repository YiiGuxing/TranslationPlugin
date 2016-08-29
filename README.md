Translation Plugin
==================

Android Studio/IntelliJ IDEA 翻译插件,可中英互译。

![screenshots](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/screenshots.gif)

![截图1](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/0.png)

![截图2](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/1.png)


安装
------------

- 使用**IDEA**内置插件系统:
  - <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Browse repositories...</kbd> > <kbd>搜索并找到"Translation"</kbd> > <kbd>Install Plugin</kbd>
- 手动:
  - 下载[`最新发布的插件包`](https://github.com/YiiGuxing/TranslationPlugin/releases/latest) -> <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Install plugin from disk...</kbd>

重启**IDEA**.

最新开发版下载: [**`TranslationPlugin-v1.2.1-SNAPSHOT.jar`**](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/TranslationPlugin-v1.2.1-SNAPSHOT.jar)

使用
------------

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

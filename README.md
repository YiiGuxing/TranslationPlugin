# **Translation Plugin**

Android Studio/IntelliJ IDEA 翻译插件,可中英互译。


![截图1](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/0.png)

![截图2](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/1.png)


## 使用方法

- **下载**：[**TranslationPlugin-v1.2.jar**](https://github.com/YiiGuxing/TranslationPlugin/raw/master/TranslationPlugin-v1.2.jar)

- **安装**：Settings(OS X:Preferences) -> Plugins -> Install plugin from disk -> 选择TranslationPlugin-v1.2.jar并安装。

- **打开翻译对话框**：点击工具栏上的 ![图标](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/2.png) 图标即可打开翻译对话框。

- **翻译编辑器中选择的文本**：在编辑器中选择文本后单击鼠标右键，再点击Translate项：

  ![截图3](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/master/images/3.png)


- **设置快捷键**：Settings(OS X:Preferences) -> Keymap -> 搜索Translate。搜索结果会有两个，Translate和TranslationDialog，Translate会取当前编辑器中选择的文本进行翻译（截图1），而TranslationDialog只会打开翻译对话框，不会取词。最后在需要添加快捷键项上右键 -> add Keyboard Shortcut 设置快捷键（默认无快捷键，按ESC键可关闭翻译对话框）。
- **设置有道API KEY**：由于有道的API在查询请求数量上存在限制，如果在1小时内查询请求次数达到一定数量后将会暂时禁止查询一段时间（大概1小时）。如果很多人同时使用同一个KEY，可能会很容易就达到了限制条件，这就可以通过使用自己的KEY来避免（一人一个KEY基本足够用了）。可在这里配置KEY —— Settings(OS X:Preferences) -> Other Settings -> Translation。如何获取有道API KEY？点击Translation设置页面上的 **“获取有道API KEY”** 或者 [**点击这里**](http://fanyi.youdao.com/openapi?path=data-mode) 可申请有道API KEY。


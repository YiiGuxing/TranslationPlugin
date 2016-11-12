# Change Log

## v1.3.2

- 用户体验优化

## [v1.3.1](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.3.1)(2016-11-7)

- 网络请求优化，支持网络代理 [#21][#21]
- 修复：气泡重叠显示

[#21]:https://github.com/YiiGuxing/TranslationPlugin/issues/21 "支持网络代理"

## [v1.3.0](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.3.0)(2016-10-12)

- 单词朗读
- UI优化
- 气泡中添加右键操作菜单
- 右键菜单翻译操作取词模式配置
- 历史记录列表中显示简要的翻译信息

## [v1.2.2](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.2.2)(2016-09-27)

- UI适配
- 自动取词
- 气泡跟随编辑器滚动而滚动
- 使用Java1.6编译
- 添加默认快捷键，见[Actions][actions]
- 修复: PluginException: cannot create class "cn.yiiguxing.plugin.translate.action.**" [#13][#13] [#14][#14]
- 修复: 与ideaVim插件ESC键冲突问题 [#18][#18]

[#13]:https://github.com/YiiGuxing/TranslationPlugin/issues/13 "Translation threw an uncaught PluginException"
[#14]:https://github.com/YiiGuxing/TranslationPlugin/issues/14 "Translate Error"
[#18]:https://github.com/YiiGuxing/TranslationPlugin/issues/18 "对话框在非聚焦状态下按ESC键关闭对话框的问题"
[actions]:https://github.com/YiiGuxing/TranslationPlugin/tree/v1.2.2#actions "Actions"

## [v1.2.1](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.2.1)(2016-08-29)

- 添加默认主题下的色彩样式
- 优化
  - 在翻译话框显示的情况下，同步翻译结果和历史记录。
  - 历史记录优化——鼠标在翻译文本过长而无法完全展示内容的历史记录条上悬停时，显示全部内容。
- Bug修复
  - 在非聚焦状态下按ESC键无法关闭对话框。
  - 翻译话框无法显示错误信息。
  - 其他Bug。

## [v1.2](https://github.com/YiiGuxing/TranslationPlugin/tree/v1.2)(2016-08-25)

- UI优化
  - 新的UI——取词翻译不再使用对话框，而使用新的气泡样式，翻译完成后不可再编辑。但可点击右上角的大头钉按钮（鼠标在气泡上才会显示）打开翻译对话框，此时可以编辑、查询历史记录。
  - 新的交互——在查询时添加等待动画。
- 翻译对话框逻辑优化
  - 打开翻译对话框不再取词。
  - 点击对话框外不再自动关闭对话框，需要手动点击对话框右上角的关闭按钮（鼠标在对话框上才会显示）或者按键盘上的ESC按钮才会关闭，方便与编辑器交互。
- 添加设置页，配置有道API KEY
- 支持单词拆分。翻译变量名或方法名时更方便
- Bug修复

# Translation Plugin Changelog

## [Unreleased]

## [3.5.4] (2023/09/13)

- The "Translate and Replace" action is now displayed by default in the context menu.
- Fixed translation errors caused by changes in Google Translate data structure.
- “翻译并替换”操作现在默认显示在上下文菜单上
- 修复了因谷歌翻译数据结构变化所导致的翻译出错的问题

## [3.5.3] (2023/09/01)

- Fixed the problem that the translation engine could not be switched when installing the plugin for the first time.
- Fixed the problem that Alibaba Translate could not parse translations in some cases.
- 修复了首次安装插件时无法切换翻译引擎的问题
- 修复了阿里翻译在某些情况下无法解析翻译的问题

## [3.5.2] (2023/08/16)

- Bug fixes.
- Bug 修复

## [3.5.1] (2023/07/03)

- Bug fixes.
- Bug 修复

## [3.5.0] (2023/06/01)

- Added OpenAI Translator engine (Experimental).
- Added custom translation server configuration for Google Translate engine.
- Added support for document translation to Youdao Translate engine.
- Added configuration for domain-specific translation for Youdao Translate engine.
- Simplified the list of languages supported by Youdao Translate engine.
- Optimized the "Translate and Replace" operation.
- Bug fixes.
- 新增 OpenAI 翻译引擎（实验性）
- Google 翻译引擎新增自定义翻译服务器配置
- 有道翻译引擎新增文档翻译支持
- 有道翻译引擎新增领域化翻译配置
- 精简了有道翻译引擎支持的语言列表
- 优化了“翻译并替换”操作
- Bug 修复

## [3.4.2] (2023/03/20)

- Compatible with IDEA 2023.1+.
- Fixed the problem where translating documentation would display HTML tag text as actual HTML elements.
- Other known bug fixes.
- 兼容 IDEA 2023.1+ 版本
- 修复了翻译文档会将 HTML 标记文本显示为实际 HTML 元素的问题
- 其他已知问题修复

## [3.4.1] (2023/01/02)

- Fixed the problem of invalid font settings.
- Other known bug fixes.
- 修复了字体设置无效的问题
- 其他已知问题修复

## [3.4.0] (2022/11/30)

- Added Microsoft translation engine. (Experimental)
- Added DeepL translation engine.
- Optimized documentation translation, added independent translation status for each documentation. (Experimental)
- The wordbook can set the storage path and supports multi-device synchronization.
- Optimized the wordbook service and interaction experience.
- Fixed the problem that the translation window was displayed abnormally when the new UI was enabled in macOS.
- Other known bug fixes.
- 新增微软翻译引擎（实验性）
- 新增DeepL翻译引擎
- 优化文档翻译，为每个文档添加独立的翻译状态（实验性）
- 单词本可以设置存储路径，并支持网盘多设备同步
- 优化单词本服务及交互体验
- 修复macOS下启用新UI时翻译窗口显示异常的问题
- 其他已知问题修复

## [3.3.5] (2022/09/04)

- 修复阿里翻译IO错误的问题
- 其他已知问题修复

## [3.3.4] (2022/07/10)

- 已知问题修复
- 文档渲染视图上下文菜单项在翻译失败时重置状态

## [3.3.3] (2022/07/01)

- 已知问题修复
- 一些优化与改进
- 新UI标题工具栏添加显示翻译对话框图标

## [3.3.2] (2022/05/17)

- 一些优化与改进
- 修复了一些问题

## [3.3.1] (2022/04/20)

- 一些优化与改进
- 修复了谷歌文档翻译未知错误的问题
- 修复了Material Theme UI主题导致翻译对话框UI显示异常的问题
- 还修复了其他好多问题

## [3.3.0] (2022/03/25)

- 全新的错误提示面板
- 翻译引擎列表显示未激活的翻译引擎
- 翻译窗口状态栏显示翻译状态
- 新增“翻译并替换”操作指示器
- 新增错误报告功能（实验性）
- 新增Rust语言的文档翻译支持
- 移除CSharp和ObjectiveC的右键文档翻译的支持
- 修复翻译快捷键在翻译对话框上无法使用的问题

## [3.2.2] (2022/01/25)

- 修复了一些错误

## [3.2.1] (2021/12/17)

- 修复了零宽度空格的问题
- 修复了快速文档窗口上无法使用快捷键进行翻译的问题

## [3.2.0] (2021/10/10)

- 新增阿里翻译引擎
- 新增文档渲染视图选择翻译的支持
- 谷歌翻译新增单词示例展示
- 谷歌翻译新增对土库曼语和卢旺达语的支持
- 取消了“翻译与替换”上下文菜单项的默认显示
- 修复了有道翻译引擎无法解析det.词性的问题
- 修复了SQL文档不能正常翻译的问题
- 修复了其他若干的问题

## [3.1.1] (2021/05/07)

- 修复了一些错误

## [3.1.0] (2021/04/13)

- 文档上下文菜单添加了文档翻译操作（IDE 2020.3+）
- 原文与译文文本区域滚动同步
- 用户体验优化调整
- 修复了一些问题

## [3.0.3] (2021/03/24)

- 使用在线发行说明
- 修复了谷歌文档翻译解析错误的问题

## [3.0.2] (2021/03/19)

- 修复了谷歌文档翻译解析错误的问题
- 修复了MacOS(M1)中单词本驱动的一个错误
- 修复了Android Studio中点击有道、百度翻译账号申请链接时崩溃的问题
- 其他一些BUG的修复

## [3.0.1] (2020/12/31)

- 恢复了右键文档翻译功能
- 按ESC键时关闭翻译窗口
- 优化了目标语言的选择逻辑
- 修复了翻译窗口导致CPU高占用率的问题

## [3.0.0] (2020/12/05)

- 新的Logo
- 新的用户界面
- 添加磁盘缓存支持
- 在翻译对话框、翻译弹出窗口界面上添加了一些操作快捷键
- 单词本添加多选操作的支持
- 快速文档翻译内容长度限制
- 修复了一些Bug

## [2.9.2] (2020/08/15)

- 添加快速切换自动文档翻译的快捷键
- 修复了一些Bug

## [2.9.1] (2020/05/18)

- 增强了运行控制台的取词翻译
- 修复了一些Bug

## [2.9.0] (2020/05/16)

- 增加了对 *Quick Documentation* 翻译的支持（感谢 [Nikolay Tropin](https://github.com/niktrop) ）
- 单词本支持导出为txt文本（感谢 [Kaiattrib](https://github.com/kaiattrib) ）

## [2.8.1] (2020/04/07)

- 修复了一些Bug

## [2.8.0] (2020/03/23)

- 谷歌翻译新增拼写检查功能
- 翻译与替换功能支持自动单词拆分
- 单词本自动聚焦新增的单词

## [2.7.3] (2020/02/22)

- 修复了一些Bug

## [2.7.2] (2020/01/21)

- 修复了Dart语言中无法对类的第一个成员的文档注释进行文档翻译的问题

## [2.7.1] (2020/01/13)

- 优化了单行文档注释的交互体验
- 修复了Go语言在一些情况下无法进行文档翻译的问题

## [2.7.0] (2020/01/08)

- 新增对Go, Dart, Python, C, C++, Objective-C/C++语言的文档注释翻译支持
- 支持列选择模式的翻译

## [2.6.2] (2019/12/16)

- 修复了导致插件无法被初始化的致命错误

## [2.6.1] (2019/12/10)

- 修复了文档注释翻译窗口大小异常的问题

## [2.6.0] (2019/12/05)

- 新增了文档注释翻译功能
- 更新了有道翻译支持语言列表，支持超过100种语言
- 优化了字体预览
- 修复了使用百度翻译引擎翻译带有换行内容时显示翻译内容不全的问题

## [2.5.1] (2019/11/01)

- 修复了导致在Android Studio上崩溃的一个致命错误
- 修复了忽略内容在一些特定的情况下会失效的问题
- 修复了重复显示每日单词对话框会导致界面异常的问题
- 一些功能增强与优化

## [2.5.0] (2019/10/20)

- 全新的有道词典视图
- 新增单词本单词标签功能，为单词本中的单词分组和归类
- 新增单词本导入导出功能
- UI主题适配
- 升级了有道翻译API
- 其他一些优化

## [2.4.2] (2019/09/29)

- 修复了在Android Studio上切换翻译引擎时崩溃的问题
- 修复了在Android Studio中点击通知上的Action时崩溃的问题
- 界面上的一个小优化

## [2.4.1] (2019/09/22)

- 修复了导致在2019.2.3版本上崩溃的一个致命错误
- 修复了导致在2019.3版本上崩溃的一个致命错误
- 修复了其他的一些BUG

## [2.4.0] (2019/09/16)

- 添加版本发行说明
- 新增单词本功能
- 优化了一些图标
- 修复了一些BUG

## [2.3.8] (2019/07/30)

- 修复翻译长文本时显示不全的问题

## [2.3.7] (2019/07/22)

- 优化交互检验
- 修复对话框上不能使用翻译替换的问题

## [2.3.6] (2019/07/15)

- 修复Google翻译Forbidden的问题
- BUG修复

## [2.3.5] (2019/07/15)

- 修复Google翻译Forbidden的问题

## [2.3.4] (2019/07/12)

- BUG修复

## [2.3.3] (2019/07/10)

- 优化交互体验
- 翻译窗口可调整尺寸
- BUG修复

## [2.3.2] (2019/02/20)

- 添加自动播放TTS支持
- BUG修复

## [2.3.1] (2019/02/11)

- 优化交互体验
- 翻译替换：添加目标语言选择配置

## [2.3.0] (2019/01/26)

- 翻译：添加目标语言选择配置
- 翻译替换：添加分隔符配置
- 翻译替换：扩大使用范围，任何文本输入框都可以使用了

## [2.2.0] (2018/11/03)

- 翻译替换：支持多种语言
- 有道翻译：显示词形

## [2.1.1] (2018/08/18)

- 语言列表排序
- 保留文本格式选项
- BUG修复

## [2.1.0] (2018/07/30)

- 实时翻译
- 忽略内容配置
- BUG修复

## [2.0.3] (2018/04/20)

- 添加百度翻译
- 翻译原文折叠
- 优化交互体验
- BUG修复

## [2.0.2] (2018/02/07)

- 多语言支持：添加英语语言
- 添加状态栏图标显示配置
- BUG修复

## [2.0.1] (2018/01/21)

- 加入状态栏图标
- BUG修复

## [2.0.0] (2018/01/15)

- 接入谷歌翻译
- 用户体验优化

## [1.3.6] (2017/07/05)

- Bug修复

## [1.3.5] (2017/06/25)

- 翻译API升级
- Bug修复

## [1.3.4] (2017/4/17)

- 支持自定义字体，再也不怕小方块了
- 使用公共API KEY时警告
- "频繁请求"和"API KEY错误"提示添加跳转至设置页链接以方便设置API KEY
- 优化历史记录
- Bug修复

## [1.3.3] (2017/2/5)

- 优化翻译与替换操作
- 修复:图像背景被应用到气泡上
- 修复:翻译替换出现非释义性中文字符
- 修复:右击可点击文本时跳转操作被执行
- 修复:NoSuchMethodError: com.intellij.util.ui.JBUI.scaleFontSize(I)I

## [1.3.2] (2016/11/29)

- 用户体验优化
- 历史记录持久化
- 翻译结果中的英文单词添加点击跳转操作
- 添加翻译替换操作
- 添加文本组件翻译操作。快速文档面板、提示气泡、输入框等也可以取词翻译。
- 扩大事件的响应范围。更多的编辑器可以响应翻译快捷键事件，二级对话框中也能通过快捷键打开翻译对话框。

## [1.3.1] (2016/11/7)

- 网络请求优化，支持网络代理
- 修复：气泡重叠显示

## [1.3.0] (2016/10/12)

- 单词朗读
- UI优化
- 气泡中添加右键操作菜单
- 右键菜单翻译操作取词模式配置
- 历史记录列表中显示简要的翻译信息

## [1.2.2] (2016/09/27)

- UI适配
- 自动取词
- 气泡跟随编辑器滚动而滚动
- 使用Java1.6编译
- 添加默认快捷键
- 修复: PluginException: cannot create class "cn.yiiguxing.plugin.translate.action.**"
- 修复: 与ideaVim插件ESC键冲突问题

## [1.2.1] (2016/08/29)

- 添加默认主题下的色彩样式
- 在翻译话框显示的情况下，同步翻译结果和历史记录。
- 历史记录优化——鼠标在翻译文本过长而无法完全展示内容的历史记录条上悬停时，显示全部内容。
- 修复在非聚焦状态下按ESC键无法关闭对话框。
- 修复翻译话框无法显示错误信息。
- 修复其他Bug。

## [1.2.0] (2016/08/25)

- 新的UI——取词翻译不再使用对话框，而使用新的气泡样式，翻译完成后不可再编辑。但可点击右上角的大头钉按钮（鼠标在气泡上才会显示）打开翻译对话框，此时可以编辑、查询历史记录。
- 新的交互——在查询时添加等待动画。
- 打开翻译对话框不再取词。
- 点击对话框外不再自动关闭对话框，需要手动点击对话框右上角的关闭按钮（鼠标在对话框上才会显示）或者按键盘上的ESC按钮才会关闭，方便与编辑器交互。
- 添加设置页，配置有道API KEY
- 支持单词拆分。翻译变量名或方法名时更方便
- Bug修复

[Unreleased]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.5.4...HEAD
[3.5.4]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.5.3...v3.5.4
[3.5.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.5.2...v3.5.3
[3.5.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.5.1...v3.5.2
[3.5.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.5.0...v3.5.1
[3.5.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.4.2...v3.5.0
[3.4.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.4.1...v3.4.2
[3.4.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.4.0...v3.4.1
[3.4.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.5...v3.4.0
[3.3.5]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.4...v3.3.5
[3.3.4]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.3...v3.3.4
[3.3.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.2...v3.3.3
[3.3.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.1...v3.3.2
[3.3.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.3.0...v3.3.1
[3.3.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.2.2...v3.3.0
[3.2.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.2.1...v3.2.2
[3.2.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.2.0...v3.2.1
[3.2.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.1.1...v3.2.0
[3.1.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.1.0...v3.1.1
[3.1.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.0.3...v3.1.0
[3.0.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.0.2...v3.0.3
[3.0.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v3.0.0...v3.0.1
[3.0.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.9.2...v3.0.0
[2.9.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.9.1...v2.9.2
[2.9.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.9.0...v2.9.1
[2.9.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.8.1...v2.9.0
[2.8.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.8.0...v2.8.1
[2.8.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.7.3...v2.8.0
[2.7.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.7.2...v2.7.3
[2.7.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.7.1...v2.7.2
[2.7.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.7.0...v2.7.1
[2.7.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.6.2...v2.7.0
[2.6.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.6.1...v2.6.2
[2.6.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.6.0...v2.6.1
[2.6.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.5.1...v2.6.0
[2.5.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.5.0...v2.5.1
[2.5.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.4.2...v2.5.0
[2.4.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.4.1...v2.4.2
[2.4.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.4.0...v2.4.1
[2.4.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.8...v2.4.0
[2.3.8]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.7...v2.3.8
[2.3.7]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.6...v2.3.7
[2.3.6]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.5...v2.3.6
[2.3.5]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.4...v2.3.5
[2.3.4]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.3...v2.3.4
[2.3.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.2...v2.3.3
[2.3.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.1...v2.3.2
[2.3.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.3.0...v2.3.1
[2.3.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.1.1...v2.2.0
[2.1.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.1.0...v2.1.1
[2.1.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.0.3...v2.1.0
[2.0.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.6...v2.0.0
[1.3.6]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.5...v1.3.6
[1.3.5]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.4...v1.3.5
[1.3.4]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.3...v1.3.4
[1.3.3]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.2...v1.3.3
[1.3.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.2.2...v1.3.0
[1.2.2]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.2.1...v1.2.2
[1.2.1]: https://github.com/YiiGuxing/TranslationPlugin/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/YiiGuxing/TranslationPlugin/commits/v1.2.0

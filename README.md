# 简介 :id=introduction

**IntelliJ Translation Plugin** 是一个适用于基于 IntelliJ 的 IDE 的翻译插件。它集成了谷歌翻译、微软翻译、DeepL 翻译、OpenAI 翻译、有道翻译等众多翻译引擎，允许您随时在 IDE 中直接翻译代码中的任何文本，如代码注释和代码文档。

![IntelliJ Translation Plugin](/img/translation_plugin.png)

## 功能 :id=features

- 多翻译引擎
  - 谷歌翻译
  - 微软翻译
  - 有道翻译
  - 百度翻译
  - 阿里翻译
  - DeepL 翻译
  - OpenAI 翻译，兼容：
    - DeepSeek
    - Doubao
    - Gemini
    - Kimi
    - Ollama
    - Qwen
    - ...
- 多语言互译
- 语音朗读
  - 微软 Edge TTS
  - 谷歌 TTS
  - OpenAI TTS
- 文档翻译
- 文本翻译与替换
- 自动选词
- 自动单词拆分
- 单词本

## 兼容 :id=compatibility

插件支持的 IDE 产品：
- Android Studio
- Aqua
- AppCode
- CLion
- DataGrip
- DataSpell
- GoLand
- HUAWEI DevEco Studio
- IntelliJ IDEA Community
- IntelliJ IDEA Ultimate
- MPS
- PhpStorm
- PyCharm Community
- PyCharm Professional
- Rider
- RubyMine
- RustRover
- WebStorm


# 快速开始 :id=quick-start

## 安装插件 :id=installation

<div class="button--plugin-installation">
  <iframe src="https://plugins.jetbrains.com/embeddable/install/8579" frameborder="none"></iframe>
</div>

**使用 IDE 内置插件系统安装（推荐）：**
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>**Marketplace**</kbd> > 搜索 **Translation** > 点击 <kbd>**Install**</kbd> 安装。
**手动安装：**
- 到 [GitHub Releases][gh:releases] 或者 [JetBrains Marketplace][plugin:versions]上下载与你的 IDE 兼容的最新版本的插件包；
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>⚙</kbd> > <kbd>**从磁盘安装插件...**</kbd> > 选择插件包并安装（无需解压）。

安装好后重新启动 **IDE** 即可。

## 开始使用 :id=usage

#### 1. 注册翻译服务帐号（可选） :id=usage-sing-up

大多数翻译服务都需要注册账号才能访问他们的服务（如：OpenAI、DeepL、有道翻译等）。因此您可能需要注册一个帐号，并获取其**认证密钥**，
然后在插件内绑定**认证密钥**：<kbd>**Preferences(Settings)**</kbd> > <kbd>**Tools**</kbd> > <kbd>
  **Translation**</kbd> > <kbd>**常规**</kbd> > <kbd>**翻译引擎**</kbd> > <kbd>**配置...**</kbd>

#### 2. 开始翻译 :id=usage-start-translating

选择文本或者鼠标指向文本 > 单击<kbd>**鼠标右键**</kbd> > <kbd>**翻译**</kbd>。

![开始翻译](/img/translate.gif ':size=520x450')

?> 或者你可以使用快捷键进行翻译，详见 [动作](#translate-action)。

#### 3. 翻译并替换 :id=usage-translate-and-replace

翻译目标文本并将其替换。当翻译目标语言为英文时，会分别输出为`骆驼式输出`、`含单词分隔符输出`（输出包含多个单词时）和`原输出`。

?> *用法：*选择文本或者鼠标指向文本 > 单击<kbd>**鼠标右键**</kbd> > <kbd>**翻译和替换...**</kbd>（仅编辑器，文本输入框请使用快捷键，详见 [动作](#translate-and-replace-action)）。

_编辑器：_

![编辑器：翻译和替换](/img/translation_replacement.gif ':size=400x380')

_文本输入框：_

![文本输入框：翻译和替换](/img/translation_replacement_component.gif ':size=460x400')

_翻译前选择语言：_

![翻译前选择语言](/img/language_selection.gif ':size=680x620')

?> _开启右键菜单项：_<kbd>**插件设置**</kbd> > <kbd>**翻译并替换**</kbd> > 勾选<kbd>**在右键菜单显示替换操作**</kbd>；  
_开启翻译前选择语言：_<kbd>**插件设置**</kbd> > <kbd>**翻译并替换**</kbd> > 勾选<kbd>**翻译前选择语言**</kbd>；  
_分隔符配置：_<kbd>**插件设置**</kbd> > <kbd>**翻译并替换**</kbd> > <kbd>**分隔符**</kbd>。

#### 4. 翻译文档 :id=usage-translate-doc

- 在文档视图（包括编辑器内嵌文档视图）或文档注释块内单击<kbd>**鼠标右键**</kbd> > <kbd>**翻译文档**</kbd>，或者使用快捷键 <kbd>**Ctrl + Shift + Q**</kbd>（macOS：<kbd>**Control + Q**</kbd>），即可切换翻译文档。
- 当开启**自动翻译文档**后，在你查看文档时文档会被自动翻译。

_快速文档：_

![文档翻译](/img/docs_translation.gif?v1.0 ':size=580x380')

_快捷键操作：_

![文档翻译](/img/docs_translation2.gif ':size=680x520')

_编辑器内联文档视图：_

![编辑器内嵌文档翻译](/img/docs_inlay_comment_translation.gif?v1.0 ':size=500x350')

_批量翻译内联文档视图：_

![批量翻译内联文档视图](/img/batch-inline-doc-translation.gif ':size=550x350')

?> _开启自动翻译文档：_<kbd>**插件设置**</kbd> > <kbd>**其他**</kbd> > 勾选<kbd>**自动翻译文档**</kbd>。

!> *注意：*编辑器内嵌文档不支持自动翻译文档。

#### 5. 切换引擎 :id=usage-switch-engines

点击 **IDE** 状态栏的引擎状态图标或者使用快捷键<kbd>**Ctrl + Shift + S**</kbd>（macOS: <kbd>**Control + Meta + Y**</kbd>）可以快速切换翻译引擎和 TTS 引擎。

![翻译引擎](/img/translation_engines.png ':size=139x314')

[gh:releases]: https://github.com/YiiGuxing/TranslationPlugin/releases
[plugin:versions]: https://plugins.jetbrains.com/plugin/8579-translation/versions


# 动作 :id=actions

#### 1. 显示翻译对话框... :id=show-trans-dialog-action

打开翻译对话框。默认显示在工具栏上。默认快捷键：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![翻译对话框](/img/translation_dialog.png ':size=550x250')

#### 2. 翻译 :id=translate-action

取词并翻译。如果有已选择的文本，优先从选择的文本内取词，否则默认以最大范围自动取词（该取词模式可在Settings中配置）。默认显示在编辑器右键菜单上，默认快捷键：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![翻译](/img/translate_auto_selection.gif ':size=300x250')

#### 3. 翻译（包含） :id=translate-inclusive-action

取词并翻译。自动以最大范围取最近的所有词，忽略手动选择的文本。默认快捷键：（无）

![翻译（包含）](/img/translate_inclusive.gif ':size=300x250')

#### 4. 翻译（独占） :id=translate-exclusive-action

取词并翻译。自动取最近的单个词，忽略手动选择的文本。默认快捷键：（无）

![翻译（独占）](/img/translate_exclusive.gif ':size=300x250')

#### 5. 翻译并替换... :id=translate-and-replace-action

翻译文本并替换。在编辑器和文本输入框内可用，取词方式同[翻译操作](#translate-action)。默认快捷键：
- Windows - <kbd>**Ctrl + Shift + X**</kbd>
- macOS - <kbd>**Control + Meta + O**</kbd>

_编辑器：_

![编辑器：翻译和替换](/img/translation_replacement_by_shortcut.gif ':size=260x380')

_文本输入框：_

![文本输入框：翻译和替换](/img/translation_replacement_component.gif ':size=460x400')

_翻译前选择语言：_

![翻译前选择语言](/img/language_selection.gif ':size=680x620')

?> _提示：_ 翻译前选择语言功能默认不启用，您需要前往配置页面开启：<kbd>**插件设置**</kbd> > <kbd>**翻译并替换**</kbd> > 勾选<kbd>**翻译前选择语言**</kbd>。

#### 6. 翻译文档 :id=translate-doc-action
##### 6.1. 切换快速文档翻译 :id=toggle-quick-doc-translation-action

快速文档中将文档内容在译文和原文之间切换。默认显示在文档视图的右键菜单和工具栏上，窗口聚焦于快速文档弹出窗或者文档工具窗口时可用。默认快捷键：
- Windows - <kbd>**Ctrl + Shift + Q**</kbd>
- macOS - <kbd>**Control + Q**</kbd>

![文档翻译](/img/docs_translation.gif ':size=302x162 :class=round')

##### 6.2. 翻译文档注释 :id=translate-doc-comment-action

翻译文档注释内容。默认显示在编辑器右键菜单上，在文档注释块内时可用。默认快捷键：（无）

_文档注释：_

![文档翻译](/img/doc_comment_translation.gif ':size=400x380')


_编辑器内嵌文档视图：_

![编辑器内嵌文档翻译](/img/docs_inlay_comment_translation.gif ':size=400x300')

#### 7. 翻译文本组件 :id=translate-text-component-action

翻译一些文本组件（如快速文档、提示气泡、输入框……）中选中的文本，不支持自动取词。默认快捷键(同[翻译操作](#translate-action))：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

#### 8. 切换引擎 :id=switch-engine-action

快速切换翻译引擎和 TTS 引擎。默认快捷键：
- Windows - <kbd>**Ctrl + Shift + S**</kbd>
- macOS - <kbd>**Control + Meta + Y**</kbd>

![翻译引擎](/img/translation_engines.png ':size=139x314')

#### 9. 每日一词 :id=word-of-the-day-action

显示每日单词对话框。默认快捷键：（无）

![每日一词](/img/word_of_the_day.png ':size=552x477 :class=round')

#### 10. 其他 :id=other-actions

- **翻译对话框快捷键：**
  - 显示源语言列表 - <kbd>**Alt + S**</kbd>
  - 显示目标语言列表 - <kbd>**Alt + T**</kbd>
  - 交换语言 - <kbd>**Alt + Shift + S**</kbd>
  - 切换窗口固定状态 - <kbd>**Alt + P**</kbd>
  - 播放TTS - <kbd>**Alt/Meta/Shift + Enter**</kbd>
  - 收藏到单词本 - <kbd>**Ctrl/Meta + F**</kbd>
  - 显示历史记录 - <kbd>**Ctrl/Meta + H**</kbd>
  - 复制译文 - <kbd>**Ctrl/Meta + Shift + C**</kbd>
  - 清空输入 - <kbd>**Ctrl/Meta + Shift + BackSpace/Delete**</kbd>
  - 展开更多翻译 - <kbd>**Ctrl/Meta + Down**</kbd>
  - 收起更多翻译 - <kbd>**Ctrl/Meta + UP**</kbd>
- **翻译气泡快捷键：**
  - 以对话框打开 - <kbd>**Ctrl + Shift + Y**</kbd> / <kbd>**Control + Meta + U**</kbd>
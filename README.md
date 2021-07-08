
# [![TranslationPlugin](./pluginIcon.svg)](https://github.com/YiiGuxing/TranslationPlugin) TranslationPlugin

[![Jetbrains Plugins][plugin-img]][plugin]
[![License][license-img]][license]
[![Build Status][build-img-master]][travis-ci]
[![GitHub release][release-img]][latest-release]
[![Version][version-img]][plugin]
[![Downloads][downloads-img]][plugin]
[![Financial Contributors on Open Collective][open-collective-badge]][open-collective]

<p align="center"><b>Translation plugin for IntelliJ based IDEs/Android Studio.</b></p>
<p align="center"><img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/screenshots.gif" alt="screenshots"></p>

## Features

- 多翻译引擎
  - Google翻译
  - 有道翻译
  - 百度翻译
- 多语言互译
- 文档翻译
- 语音朗读
- 自动选词
- 自动单词拆分
- 单词本

## Compatibility:

- Android Studio
- AppCode
- CLion
- DataGrip
- GoLand
- IntelliJ IDEA Ultimate
- IntelliJ IDEA Community
- IntelliJ IDEA Educational
- MPS
- PhpStorm
- PyCharm Professional
- PyCharm Community
- PyCharm Educational
- Rider
- RubyMine
- WebStorm

## Sponsors

<table>
      <td>
        <a href="https://www.jetbrains.com/?from=TranslationPlugin" target="_blank">
            <img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/jetbrains.svg" alt="JetBrains" title="Development powered by JetBrains.">
        </a>
      </td>
      <td>
        <a href="https://sponsorlink.codestream.com/?utm_source=jbmarket&utm_campaign=translation&utm_medium=banner" target="_blank">
            <img src="https://alt-images.codestream.com/codestream_logo_translation.png" alt="CodeStream" title="CodeStream">
        </a>
      </td>
</table>

## Installation

<a href="https://plugins.jetbrains.com/plugin/8579-translation" target="_blank">
    <img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/button-install.png" alt="Get from Marketplace" title="Get from Marketplace">
</a>

- **使用 IDE 内置插件系统安装:**
  - <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>搜索并找到<b>"Translation"</b></kbd> > <kbd>Install</kbd>

- **手动安装:**
  - 下载[插件包][latest-release] -> <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> -> 选择插件包并安装（无需解压）

重启**IDE**.

## Usage

1. **申请有道智云翻译服务（可选）:**
   - 注册[有道智云](http://ai.youdao.com)帐号并获取其**应用ID**和**应用密钥**
   - 绑定**应用ID**和**应用密钥**：<kbd>Preferences(Settings)</kbd> > <kbd>\[Other Settings]</kbd> > <kbd>Translation</kbd> > <kbd>有道翻译</kbd>

   注：请注意保管好你的**应用密钥**，防止其泄露。如帐号欠费，将无法使用。

2. **申请百度翻译服务（可选）:**
   - 注册[百度翻译开放平台](http://api.fanyi.baidu.com/api/trans/product/desktop?req=developer)帐号并获取其**应用ID**和**应用密钥**
   - 绑定**应用ID**和**应用密钥**：<kbd>Preferences(Settings)</kbd> > <kbd>\[Other Settings]</kbd> > <kbd>Translation</kbd> > <kbd>百度翻译</kbd>

   注：请注意保管好你的**应用密钥**，防止其泄露。如帐号欠费，将无法使用。

3. **开始翻译:**

   <kbd>选择文本或者鼠标指向文本</kbd> > <kbd>单击鼠标右键</kbd> > <kbd>Translate</kbd>

   或者使用快捷键进行翻译，详见 **[Actions](#actions)**

4. **翻译并替换:**

   翻译目标文本并将其替换。当翻译目标语言为英文时，会分别输出为**骆驼式输出**、**含单词分隔符输出**（输出包含多个单词时，分隔符可在插件配置页面中配置：翻译设置 > 分隔符）和**原输出**。

   用法：<kbd>选择文本或者鼠标指向文本</kbd> > <kbd>单击鼠标右键</kbd> > <kbd>Translate and Replace...</kbd>（或者使用快捷键进行翻译，详见 **[Actions](#actions)**）。

5. **切换翻译引擎:**

   点击状态栏的翻译引擎状态图标或者使用快捷键 <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>S</kbd>（Mac OS: <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>Y</kbd>）可以快速切换翻译引擎，目前有谷歌翻译、有道翻译和百度翻译。


## Actions

- **Show Translation Dialog...**

  打开翻译对话框。默认显示在工具栏上。默认快捷键:

  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>O</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>I</kbd>

- **Translate**

  取词并翻译。如果有已选择的文本，优先从选择的文本内取词，否则默认以最大范围自动取词（该取词模式可在Settings中配置）。默认显示在编辑器右键菜单上，默认快捷键:

  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Translate(Inclusive)**

  取词并翻译。自动以最大范围取最近的所有词，忽略手动选择的文本。默认快捷键: (无)

- **Translate(Exclusive)**

  取词并翻译。自动取最近的单个词，忽略手动选择的文本。默认快捷键: (无)

- **Translate and Replace...**

  翻译并替换。取词方式同`Translate`操作。默认显示在编辑器右键菜单上，默认快捷键:

  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>X</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>O</kbd>

- **Translate Documentation**
  
  翻译文档注释内容。默认显示在编辑器右键菜单上，光标在文档注释块内时可用。默认快捷键: (无)

- **Toggle Quick Documentation Translation**
  
  快速文档中将文档内容在译文和原文之间切换。窗口聚焦于快速文档弹出窗或者文档工具窗口时可用。默认快捷键(同**Translate**): 
  
  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Translate Text Component**

  翻译一些文本组件（如快速文档、提示气泡、输入框……）中选中的文本，不支持自动取词。默认快捷键:

  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Choose Translator**

  快速切换翻译引擎。默认快捷键:

  - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>S</kbd>
  - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>Y</kbd>

- **Word of the Day**

  显示每日单词对话框。默认快捷键: (无)

- **其他**

  - 翻译对话框快捷键：
  
    - 显示源语言列表 - <kbd>Alt</kbd> + <kbd>S</kbd>
    - 显示目标语言列表 - <kbd>Alt</kbd> + <kbd>T</kbd>
    - 交换语言 - <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>S</kbd>
    - 切换窗口固定状态 - <kbd>Alt</kbd> + <kbd>P</kbd>
    - 播放TTS - <kbd>Alt/Meta/Shift</kbd> + <kbd>Enter</kbd>
    - 收藏到单词本 - <kbd>Ctrl/Meta</kbd> + <kbd>F</kbd>
    - 显示历史记录 - <kbd>Ctrl/Meta</kbd> + <kbd>H</kbd>
    - 拷贝译文 - <kbd>Ctrl/Meta</kbd> + <kbd>Shift</kbd> + <kbd>C</kbd>
    - 清空输入 - <kbd>Ctrl/Meta</kbd> + <kbd>Shift</kbd> + <kbd>BackSpace/Delete</kbd>
    - 展开更多翻译 - <kbd>Ctrl/Meta</kbd> + <kbd>Down</kbd>
    - 收起更多翻译 - <kbd>Ctrl/Meta</kbd> + <kbd>UP</kbd>

  - 翻译气泡快捷键：
  
    - 以对话框打开 - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd> / <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

  - 快速文档窗口快捷键：
  
    - 开启/关闭自动翻译 - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd> / <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

## FAQ

1. **出现网络错误或者网络连接超时怎么办？**

   **答**：请按照以下步骤操作以尝试解决问题。  
   - 检查网络环境，确保网络畅通。
   - 检查是否是因为使用了代理软件而导致插件无法访问翻译API。
   - 检查IDE代理配置，查看是否是因为IDE代理配置导致的问题。
   - 如果使用的是谷歌翻译，请检查谷歌翻译配置。由于一些众所周知的原因，导致谷歌翻译的网络访问不是很稳定。如果无法访问，可以尝试切换`使用translate.google.com`选项的勾选状态后再进行翻译。另外，TTS功能使用的也是谷歌翻译的API。

2. **翻译内容出现乱码怎么办？**

   **答**：出现乱码一般是因为字体中没有相应的字符的问题，可以到插件的设置页面修改字体以解决乱码问题（如下图所示）。
   
   ![screenshots](https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/settings_font.png)

3. **无法保存应用密钥怎么办?**

   **答**：可以尝试将密码保存方式改成`In KeePass`方式 (<kbd>Settings</kbd> > <kbd>Appearance & Behavior</kbd> > <kbd>System Settings</kbd> > <kbd>Passwords</kbd>)，原因与细节：
   - MacOS，请另阅 [#81](https://github.com/YiiGuxing/TranslationPlugin/issues/81)
   - Linux，请另阅 [#115](https://github.com/YiiGuxing/TranslationPlugin/issues/115)

4. **快捷键不能使用怎么办？**

   **答**：快捷键不能使用可能是因为被其他插件或者外部应用占用了，可以为相应的操作重新设置新的快捷键。

## Change Notes

## [v3.1.1](https://github.com/YiiGuxing/TranslationPlugin/tree/v3.1.1) (2021-05-07)

- 修复了一些错误

[完整的更新历史记录](./CHANGELOG.md)


## Support

您可以通过执行以下任意操作来贡献和支持此项目：
* 在GitHub上标星
* 将插件分享给您的小伙伴们
* 提交PR
* 反馈问题
* 提出您的想法/建议
* 如果您喜欢这个插件，请考虑捐赠以维持插件的相关活动：

  <table>
    <thead align="center">
      <tr>
        <th>Open Collective</th>
        <th>支付宝</th>
        <th>微信支付</th>
      </tr>
    </thead>
    <tr align="center">
      <td>
        <a href="https://opencollective.com/translation-plugin" target="_blank">
            <img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/open-collective.svg" width="171px" alt="OpenCollective">
        </a>
      </td>
      <td><img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/alipay.png" alt="Alipay"></td>
      <td><img src="https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/images/wechat.png" alt="WechatPlay"></td>
    </tr>
  </table>
  使用支付宝/微信支付捐赠后请留言或者通过邮件提供您的名字/昵称和网站，格式为：

  `名字/昵称 [<网站>][：留言]`（网站与留言为可选部分，例子：`Yii.Guxing <github.com/YiiGuxing>：加油！`）

  您提供的名字、网站和捐赠总额将会被添加到[**捐赠者**][financial-contributors]列表中。
  
  邮箱地址：[yii.guxing@gmail.com](mailto:yii.guxing@gmail.com?subject=Donate&body=%E5%90%8D%E5%AD%97%2F%E6%98%B5%E7%A7%B0%3C%E7%BD%91%E7%AB%99%3E%EF%BC%9A%E6%82%A8%E7%9A%84%E7%95%99%E8%A8%80%0A%0A%E6%8D%90%E8%B5%A0%E9%87%91%E9%A2%9D%EF%BC%9A%0A%E6%94%AF%E4%BB%98%E5%B9%B3%E5%8F%B0%EF%BC%9A%E6%94%AF%E4%BB%98%E5%AE%9D%2F%E5%BE%AE%E4%BF%A1%E6%94%AF%E4%BB%98%0A%E6%94%AF%E4%BB%98%E5%AE%9D%E7%94%A8%E6%88%B7%E5%90%8D%2F%E5%BE%AE%E4%BF%A1%E7%94%A8%E6%88%B7%E5%90%8D%2F%E5%8D%95%E5%8F%B7%EF%BC%88%E5%90%8E5%E4%BD%8D%EF%BC%89%EF%BC%9A%0A%0A) (点击发送邮件)

**感谢您的支持！**

## More Plugins

- [FIGlet](https://github.com/YiiGuxing/intellij-figlet)
- [Material Design Color Palette](https://github.com/YiiGuxing/material-design-color-palette)


[build-img-dev]: https://img.shields.io/travis/YiiGuxing/TranslationPlugin/dev.svg?style=flat-square
[build-img-master]: https://img.shields.io/travis/YiiGuxing/TranslationPlugin/master.svg?style=flat-square
[license-img]: https://img.shields.io/github/license/YiiGuxing/TranslationPlugin.svg?style=flat-square
[release-img]: https://img.shields.io/github/release/YiiGuxing/TranslationPlugin.svg?style=flat-square
[plugin-img]: https://img.shields.io/badge/JetBrainsPlugin-8579-orange.svg?style=flat-square
[downloads-img]: https://img.shields.io/jetbrains/plugin/d/8579.svg?style=flat-square
[version-img]: https://img.shields.io/jetbrains/plugin/v/8579.svg?style=flat-square&colorB=0091ea
[latest-release]: https://github.com/YiiGuxing/TranslationPlugin/releases/latest
[license]: https://github.com/YiiGuxing/TranslationPlugin/blob/master/LICENSE
[travis-ci]: https://travis-ci.org/YiiGuxing/TranslationPlugin
[plugin]: https://plugins.jetbrains.com/plugin/8579
[open-collective]: https://opencollective.com/translation-plugin
[open-collective-badge]: https://opencollective.com/translation-plugin/all/badge.svg?label=financial+contributors&style=flat-square&&color=D05CE3
[financial-contributors]: https://github.com/YiiGuxing/TranslationPlugin/blob/master/FINANCIAL_CONTRIBUTORS.md

## Contributors

### Code Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/YiiGuxing/TranslationPlugin/graphs/contributors"><img src="https://opencollective.com/translation-plugin/contributors.svg?width=890&button=false" /></a>

### Financial Contributors

Become a financial contributor and help us sustain our community. [[Contribute](https://opencollective.com/translation-plugin/contribute)]

#### Individuals

<a href="https://opencollective.com/translation-plugin"><img src="https://opencollective.com/translation-plugin/individuals.svg?width=890"></a>

#### Organizations

Support this project with your organization. Your logo will show up here with a link to your website. [[Contribute](https://opencollective.com/translation-plugin/contribute)]

<a href="https://opencollective.com/translation-plugin/organization/0/website"><img src="https://opencollective.com/translation-plugin/organization/0/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/1/website"><img src="https://opencollective.com/translation-plugin/organization/1/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/2/website"><img src="https://opencollective.com/translation-plugin/organization/2/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/3/website"><img src="https://opencollective.com/translation-plugin/organization/3/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/4/website"><img src="https://opencollective.com/translation-plugin/organization/4/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/5/website"><img src="https://opencollective.com/translation-plugin/organization/5/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/6/website"><img src="https://opencollective.com/translation-plugin/organization/6/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/7/website"><img src="https://opencollective.com/translation-plugin/organization/7/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/8/website"><img src="https://opencollective.com/translation-plugin/organization/8/avatar.svg"></a>
<a href="https://opencollective.com/translation-plugin/organization/9/website"><img src="https://opencollective.com/translation-plugin/organization/9/avatar.svg"></a>

#### 捐赠者

| **姓名** | **网站** | **捐赠总额** |
| -------- | -------- | ------------ |
| 唐嘉 | [github.com/qq1427998646](https://github.com/qq1427998646) | 100 CNY |
| 凌高 | | 100 CNY |
| 三分醉 | [www.sanfenzui.com](http://www.sanfenzui.com) | 88 CNY |
| LeeWyatt | [github.com/leewyatt](https://github.com/leewyatt) | 76.6 CNY |
| Him188 | [github.com/him188](https://github.com/him188) | 66 CNY |
| Sampan | [gitlab.com/SamuelPan](http://gitlab.com/SamuelPan) | 50 CNY |
| 一个李大壮罢了 | | 50 CNY |
| Songc | [www.zdone.com](https://www.zdone.com) | 50 CNY |
| Wangyangisgood | | 50 CNY |
| Pandeng | [github.com/jiafeimao-gjf](https://github.com/jiafeimao-gjf) | 50 CNY |
| 堂哥 | [codertang.com](https://codertang.com) | 50 CNY |

[更多捐赠者][financial-contributors]

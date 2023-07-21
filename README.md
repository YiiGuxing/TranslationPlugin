# [![TranslationPlugin][plugin-logo]][gh:translation-plugin] TranslationPlugin

[![Plugin Homepage][badge:plugin-homepage]][plugin-homepage]
[![Build Status][badge:build]][gh:workflows-build]
[![License][badge:license]][gh:license]
[![GitHub releases][badge:release]][gh:releases]
[![Version][badge:version]][plugin-versions]
[![Downloads][badge:downloads]][plugin-homepage]
[![Financial Contributors on Open Collective][badge:open-collective]][open-collective]


<p align="center"><b>Translation plugin for IntelliJ based IDEs/Android Studio.</b></p>
<p align="center"><img src="https://yiiguxing.github.io/TranslationPlugin/img/ext/screenshots.gif" alt="screenshots"></p>

<br/><br/><br/>

[![Getting Started][badge:get-started-en]][get-started-en]
[![开始使用][badge:get-started-zh]][get-started-zh]
[![はじめに][badge:get-started-jp]][get-started-ja]
[![시작하기][badge:get-started-ko]][get-started-ko]

---

[![Microsoft Translator][logo:microsoft-translator]](https://www.bing.com/translator)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![Google Translate][logo:google-translate]](https://translate.google.com)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![OpenAI Translator][logo:openai]](https://openai.com/)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![DeepL Translator][logo:deepl-translator]](https://www.deepl.com)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![Youdao Translate][logo:youdao-translate]](https://ai.youdao.com)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![Baidu Translate][logo:baidu-translate]](https://fanyi-api.baidu.com)
<span>&nbsp;&nbsp;&nbsp;&nbsp;</span>
[![Alibaba Translate][logo:ali-translate]](https://translate.alibaba.com)

---

- [Features](#features)
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Using the Plugin](#using-the-plugin)
- [Actions](#actions)
- [FAQ](#faq)
- [Support and Donations](#support-and-donations)
- [Contributors](#contributors)
    - [Code Contributors](#code-contributors)
    - [Financial Contributors](#financial-contributors)

## Features

- Multiple Translation Engines
    - Microsoft Translator
    - Google Translate
    - DeepL Translator
    - OpenAI Translator
    - Youdao Translate
    - Baidu Translate
    - Alibaba Translate
- Multilingual translation
- Document translation
- Text-to-speech
- Automatic word selection
- Automatic word breaks
- Word Book

## Compatibility

- Android Studio
- AppCode
- CLion
- DataGrip
- GoLand
- HUAWEI DevEco Studio
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

## Installation

<a href="https://plugins.jetbrains.com/plugin/8579-translation" target="_blank">
    <img src="https://yiiguxing.github.io/TranslationPlugin/img/ext/installation_button.svg" height="52" alt="Get from Marketplace" title="Get from Marketplace">
</a>

- **Installing from the plugin repository within the IDE:**
    - <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search and find <b>"
      Translation"</b></kbd> > <kbd>Install Plugin</kbd>.

- **Installing manually:**
    - Download the plugin package on [GitHub Releases][gh:releases] or in
      the [JetBrains Plugin Repository][plugin-versions].
    - <kbd>Preferences(Settings)</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> >
      Select the plugin package and install (no need to unzip)

Restart the **IDE** after installation.

## Using The Plugin

1. **Sign up for a translation service (optional)**

   Most translation services require user registration to access their services
   (such as OpenAI, DeepL, Youdao Translate, etc.).
   Therefore, you may need to create an account, obtain an **Authentication Key**,
   and then bind the **Authentication Key** within the plugin：<kbd>Preferences(Settings)</kbd> > <kbd>
   Tools</kbd> > <kbd>
   Translation</kbd> > <kbd>General</kbd> > <kbd>Translation Engine</kbd> > <kbd>Configure...</kbd>

2. **Begin translating**

   <kbd>Select a text or hover the mouse over the text</kbd> > <kbd>Right-click</kbd> > <kbd>Translate</kbd>

   Or use shortcuts for translation, as detailed in **[Actions](#actions)**.

3. **Translate and replace**

   Translate the target text and replace it. If the target language is English, the output has several formats: **in
   camel case, with a word separator** (when the output contains multiple words, the separator can be configured in the
   plugin configuration page: <kbd>Translation Settings</kbd> > <kbd>Translate and replace</kbd> > <kbd>Separator</kbd>)
   and in the **original format**.

   Instructions: <kbd>Select a text or hover the mouse over the text</kbd> > <kbd>Right-click</kbd> > <kbd>Translate and
   Replace...</kbd> (Or use shortcuts for translation, as detailed in **[Actions](#actions)**).

4. **Translate documents**

   <kbd>Preferences(Settings)</kbd> > <kbd>Tools</kbd> > <kbd>Translation</kbd> > <kbd>Other</kbd> > <kbd>Translate
   documents</kbd>: When you check this option, the document will be automatically translated when you view it.

5. **Switch translation engines**

   Click the translation engine icon in the status bar or use the shortcut <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>
   S</kbd> (Mac OS: <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>Y</kbd>) to switch between translation engines quickly.

## Actions

- **Show Translation Dialog...**

  Open the translation dialog, which appears by default on the toolbar. Default shortcut:

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>O</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>I</kbd>

- **Translate**

  Extract words and translate them. If you have already selected a text, extract the words from the portion of the text
  you'd like to translate. Otherwise, words are extracted automatically from the maximum range (this extraction can be
  configured in Settings). This action is displayed by default in the editor's right-click context menu. Default
  shortcut:

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Translate(Inclusive)**

  Extract words and translate them. Automatically extract and translate all words from a specific range, ignoring
  manually selected text. Default shortcut: (None)

- **Translate(Exclusive)**

  Extract words and translate them. Automatically extract the nearest single word, ignoring manually selected text.
  Default shortcut: (None)

- **Translate and Replace...**

  Translate and replace. The word extraction method works the same as when **translating**. Default shortcut:

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>X</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>O</kbd>

- **Translate Documentation**

  Translate the contents of document comments.
  This option is displayed by default in the editor's context menu (right-click to access)
  and is available when the cursor is in the document's comment block.
  Default shortcut: (None)

- **Toggle Quick Documentation Translation**

  Toggle between the original and translated texts in Quick Documentation. This option is available when the focus is on
  the Quick Documentation pop-up window or the documentation tool window. Default shortcut (same as **translation**
  shortcut):

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Translate Text Component**

  Translate selected text in some text components (e.g. Quick Docs, popup hints, input boxes...). This does not support
  automatic word extraction. Default shortcut (same as **translation** shortcut):

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>U</kbd>

- **Choose Translation Engine**

  Quickly toggle between translation engines. Default shortcut:

    - Windows - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>S</kbd>
    - Mac OS - <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>Y</kbd>

- **Word of the Day**

  Display the 'Word of the Day' dialog box. Default shortcut: (None)

- **Other**

    - Translation dialog shortcuts:

        - Display the list of source languages - <kbd>Alt</kbd> + <kbd>S</kbd>
        - Display the list of target languages - <kbd>Alt</kbd> + <kbd>T</kbd>
        - Switch between languages - <kbd>Alt</kbd> + <kbd>Shift</kbd> + <kbd>S</kbd>
        - Pin/unpin a window - <kbd>Alt</kbd> + <kbd>P</kbd>
        - Play TTS - <kbd>Alt/Meta/Shift</kbd> + <kbd>Enter</kbd>
        - Save to Word Book - <kbd>Ctrl/Meta</kbd> + <kbd>F</kbd>
        - Show history - <kbd>Ctrl/Meta</kbd> + <kbd>H</kbd>
        - Copy translation - <kbd>Ctrl/Meta</kbd> + <kbd>Shift</kbd> + <kbd>C</kbd>
        - Clear input - <kbd>Ctrl/Meta</kbd> + <kbd>Shift</kbd> + <kbd>BackSpace/Delete</kbd>
        - Expand more translations - <kbd>Ctrl/Meta</kbd> + <kbd>Down</kbd>
        - Hide more translations - <kbd>Ctrl/Meta</kbd> + <kbd>UP</kbd>

    - Translation balloon shortcuts:

        - Open dialog - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd> / <kbd>Control</kbd> + <kbd>Meta</kbd> + <kbd>
          U</kbd>

    - Quick Documentation window shortcuts:

        - Enable/disable automatic translation - <kbd>Ctrl</kbd> + <kbd>Shift</kbd> + <kbd>Y</kbd> / <kbd>Control</kbd>
            + <kbd>Meta</kbd> + <kbd>U</kbd>

## FAQ

> *If you have any questions, please ask [here][gh:discussions-q-a].*

1. **What should I do if there is a network error or the network connection times out?**

   **A:**
    - Check the network environment and make sure the network is running smoothly.
    - Check whether a proxy is preventing the plugin from accessing the translation API.
    - Check the IDE proxy configuration to see if that is the cause of the problem.

2. **What should I do if the translated content appears garbled?**

   **A:** Garbled code generally appears when there is a lack of corresponding characters in the font. You can go to
   the Settings page of the plugin to modify the font in order to fix the garbled code (as shown below).

   ![screenshots][file:settings-font]

3. **What if I can't save the application key?**

   **A:** You can try changing the way passwords are saved to `In KeePass` (<kbd>Settings</kbd> > <kbd>Appearance &
   Behavior</kbd> > <kbd>System Settings</kbd> > <kbd>Passwords</kbd>). For more details:
    - For macOS, please refer to [#81][gh:#81]
    - For Linux, please refer to [#115][gh:#115]

4. **What if the shortcuts don't work?**

   **A:** The shortcut keys are most likely not working because they are being used in other plugins or external
   applications. You can reset shortcut keys for the corresponding operations.

## Support and Donations

You can contribute and support this project by doing any of the following:

* Star the project on GitHub
* Give feedback
* Commit PR
* Contribute your ideas/suggestions
* Share the plugin with your friends/colleagues
* If you love this plugin, please consider donating. It will inspire me to continue development on the project:

  <table>
    <thead align="center">
      <tr>
        <th><a href="https://opencollective.com/translation-plugin" target="_blank">Open Collective</a></th>
        <th><a href="https://pay.weixin.qq.com/index.php/public/wechatpay_en" target="_blank">WeChat Pay</a></th>
        <th><a href="https://global.alipay.com" target="_blank">Alipay</a></th>
      </tr>
    </thead>
    <tr align="center">
      <td>
        <a href="https://opencollective.com/translation-plugin/donate" target="_blank">
          <img src="https://yiiguxing.github.io/TranslationPlugin/img/ext/donate_to_collective.svg" width=298 alt="Donate To Our Collective">
        </a>
      </td>
      <td>
        <a href="https://pay.weixin.qq.com/index.php/public/wechatpay_en" target="_blank">
          <img src="https://yiiguxing.github.io/TranslationPlugin/img/donating_wechat_pay.svg" alt="WeChat Play">
        </a>
      </td>
      <td>
        <a href="https://global.alipay.com" target="_blank">
          <img src="https://yiiguxing.github.io/TranslationPlugin/img/donating_alipay.svg" alt="Alipay">
        </a>
      </td>
    </tr>
  </table>

  > **Note**
  >
  > After using Alipay/WeChat to pay for your donation, please provide your name/nickname and website by leaving
  > a message or via email in the following format:
  >
  > `Name/Nickname [<website>][: message]` (website and message are optional.)
  >
  > Example: `Yii.Guxing <github.com/YiiGuxing>: I like the plugin!`
  >
  > If you choose to send an email, please also provide the following information:
  > ```text
  > Donation Amount: <amount>
  > Payment Platform: Alipay/WeChat Pay
  > Payment Number (last 5 digits): <number>
  > ```
  > Email address: [yii.guxing@outlook.com][mailto] (click to send email)
  >
  > The name, website and total donation amount you provide will be added to
  > the [donor list][file:financial-contributors].

**Thank you for your support!**

## Contributors

### Code Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/YiiGuxing/TranslationPlugin/graphs/contributors"><img src="https://opencollective.com/translation-plugin/contributors.svg?width=890&button=false" /></a>

### Financial Contributors

Become a financial contributor and help us sustain our
community. [[Contribute][open-collective-contribute]]

#### Backers

Thank you to all our backers! ❤️ [[Become a backer](https://opencollective.com/translation-plugin/donate)]

<a href="https://opencollective.com/translation-plugin/donate" target="_blank"><img src="https://opencollective.com/translation-plugin/individuals.svg?width=800"></a>

#### Sponsors

Support this project by becoming a sponsor! Your logo will show up here with a link to your
website. [[Become a sponsor][open-collective-contribute]]

<a href="https://opencollective.com/translation-plugin/organization/0/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/0/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/1/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/1/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/2/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/2/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/3/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/3/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/4/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/4/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/5/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/5/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/6/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/6/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/7/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/7/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/8/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/8/avatar.svg?avatarHeight=128"></a>
<a href="https://opencollective.com/translation-plugin/organization/9/website" target="_blank"><img src="https://opencollective.com/translation-plugin/organization/9/avatar.svg?avatarHeight=128"></a>

#### Donors

| **Name** | **Website**                                                | **Amount** |
|----------|------------------------------------------------------------|------------|
| 丿初音     |                                                          | 325 CNY |
| LiMingjun |                                                          | 100 CNY |
| DarknessTM | [github.com/darknesstm](https://github.com/darknesstm) | 100 CNY |
| Sunlife95 |                                                          | 100 CNY |
| 马强@咔丘互娱 |                                                          | 100 CNY |
| Rrtt_2323 |                                                            | 100 CNY    |
| 唐嘉       | [github.com/qq1427998646](https://github.com/qq1427998646) | 100 CNY    |
| 凌高       |                                                            | 100 CNY    |
| Mritd    | [mritd.com](https://mritd.com)                             | 88.88 CNY  |
| 三分醉      | [www.sanfenzui.com](http://www.sanfenzui.com)              | 88 CNY     |

[More donors][file:financial-contributors]


[plugin-logo]: https://cdn.jsdelivr.net/gh/YiiGuxing/TranslationPlugin@master/pluginIcon.svg

[badge:plugin-homepage]: https://img.shields.io/badge/plugin%20homepage-translation-4caf50.svg?style=flat-square
[badge:build]: https://img.shields.io/endpoint?label=build&style=flat-square&url=https%3A%2F%2Factions-badge.atrox.dev%2FYiiGuxing%2FTranslationPlugin%2Fbadge%3Fref%3Dmaster
[badge:license]: https://img.shields.io/github/license/YiiGuxing/TranslationPlugin.svg?style=flat-square
[badge:release]: https://img.shields.io/github/release/YiiGuxing/TranslationPlugin.svg?sort=semver&style=flat-square&colorB=0097A7
[badge:version]: https://img.shields.io/jetbrains/plugin/v/8579.svg?style=flat-square&colorB=2196F3
[badge:downloads]: https://img.shields.io/jetbrains/plugin/d/8579.svg?style=flat-square&colorB=5C6BC0
[badge:open-collective]: https://opencollective.com/translation-plugin/all/badge.svg?label=financial+contributors&style=flat-square&color=d05ce3
[badge:get-started-en]: https://img.shields.io/badge/Get%20Started-English-4CAF50?style=flat-square
[badge:get-started-zh]: https://img.shields.io/badge/%E5%BC%80%E5%A7%8B%E4%BD%BF%E7%94%A8-%E4%B8%AD%E6%96%87-2196F3?style=flat-square
[badge:get-started-jp]: https://img.shields.io/badge/%E3%81%AF%E3%81%98%E3%82%81%E3%81%AB-%E6%97%A5%E6%9C%AC%E8%AA%9E-009688?style=flat-square
[badge:get-started-ko]: https://img.shields.io/badge/%EC%8B%9C%EC%9E%91%ED%95%98%EA%B8%B0-%ED%95%9C%EA%B5%AD%EC%96%B4-7CB342?style=flat-square

[gh:translation-plugin]: https://github.com/YiiGuxing/TranslationPlugin
[gh:releases]: https://github.com/YiiGuxing/TranslationPlugin/releases
[gh:workflows-build]: https://github.com/YiiGuxing/TranslationPlugin/actions/workflows/build.yml
[gh:license]: https://github.com/YiiGuxing/TranslationPlugin/blob/master/LICENSE
[gh:discussions-q-a]: https://github.com/YiiGuxing/TranslationPlugin/discussions/categories/q-a
[gh:#81]: https://github.com/YiiGuxing/TranslationPlugin/issues/81
[gh:#115]: https://github.com/YiiGuxing/TranslationPlugin/issues/115

[logo:ali-translate]: .github/readme/ali_translate_logo.png
[logo:baidu-translate]: .github/readme/baidu_translate_logo.svg
[logo:deepl-translator]: .github/readme/deepl_translate_logo.svg
[logo:google-translate]: .github/readme/google_translate_logo.svg
[logo:microsoft-translator]: .github/readme/microsoft_translator_logo.svg
[logo:openai]: .github/readme/openai_logo.svg
[logo:youdao-translate]: .github/readme/youdao_translate_logo.svg

[file:settings-font]: .github/readme/settings_font.png
[file:financial-contributors]: https://github.com/YiiGuxing/TranslationPlugin/blob/master/FINANCIAL_CONTRIBUTORS.md

[get-started-en]: https://yiiguxing.github.io/TranslationPlugin/en/start.html
[get-started-zh]: https://yiiguxing.github.io/TranslationPlugin/start.html
[get-started-ja]: https://yiiguxing.github.io/TranslationPlugin/ja/start.html
[get-started-ko]: https://yiiguxing.github.io/TranslationPlugin/ko/start.html

[plugin-homepage]: https://plugins.jetbrains.com/plugin/8579-translation
[plugin-versions]: https://plugins.jetbrains.com/plugin/8579-translation/versions

[open-collective]: https://opencollective.com/translation-plugin
[open-collective-contribute]: https://opencollective.com/translation-plugin/contribute

[mailto]: mailto:yii.guxing@outlook.com?subject=Donate&body=Name%2FNickname%3Cwebsite%3E%3A%20%3Cmessage%3E%0D%0DDonation%20Amount%3A%20%3Camount%3E%0DPayment%20Platform%3A%20Alipay%2FWeChat%20Pay%0DPayment%20Number%20%28last%205%20digits%29%3A%20%3Cnumber%3E%0D%0D


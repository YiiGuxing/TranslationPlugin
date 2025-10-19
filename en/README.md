# Introduction :id=introduction

**IntelliJ Translation Plugin** is an IntelliJ-based IDEs/Android Studio translation plugin.
It integrates many translation engines such as Google Translate, Microsoft Translate,
OpenAI Translate, DeepL Translate, etc. You can translate the text, code comments,
and code documents, etc. You want to translate in your IDE at any time.

![IntelliJ Translation Plugin](/img/translation_plugin.png)

## Features :id=features

- Multiple Translation Engines
  - Google Translate
  - Microsoft Translator
  - DeepL Translator
  - Youdao Translate
  - Baidu Translate
  - Alibaba Translate
  - OpenAI Translator, compatible with:
    - DeepSeek
    - Doubao
    - Gemini
    - Kimi
    - Ollama
    - Qwen
    - ...
- Multiple languages inter-translation
- Text-to-speech
  - Microsoft Edge TTS
  - Google TTS
  - OpenAI TTS
- Document translation
- Text translation and replacement
- Automatic word selection
- Automatic word breaks
- Word Book

## Compatibility :id=compatibility

Supported IDE Products:
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


# Quick Start :id=quick-start

## Installation :id=installation

<div class="button--plugin-installation">
  <iframe src="https://plugins.jetbrains.com/embeddable/install/8579" frameborder="none"></iframe>
</div>

**Installing from the plugin repository within the IDE (recommend):**
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>**Marketplace**</kbd> >
  Search and find "**Translation**" > Install Plugin.
**Installing manually:**
- Download the latest plugin package compatible with your IDE on [GitHub Releases][gh:releases]
  or in the [JetBrains Marketplace][plugin:versions].
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>⚙</kbd> >
  <kbd>**Install plugins from disk...**</kbd> > Select the plugin package and install (no need to unzip).

Restart the IDE after installation.

## Using The Plugin :id=usage

#### 1. Sign up for a translation service (optional) :id=usage-sing-up

Most translation services require user registration to access their services
(such as OpenAI, DeepL, Youdao Translate, etc.).
Therefore, you may need to create an account, obtain an **Authentication Key**,
and then bind the **Authentication Key** within the plugin: <kbd>**Preferences(Settings)**</kbd> > <kbd>**Tools**</kbd> >
<kbd>**Translation**</kbd> > <kbd>**General**</kbd> > <kbd>**Translation Engine**</kbd> > <kbd>**Configure...**</kbd>

#### 2. Begin translating :id=usage-start-translating

Select a text or hover the mouse over the text > <kbd>**Right-click**</kbd> > <kbd>**Translate**</kbd>.

![Begin translating](/img/translate.gif ':size=520x450')

?> Or use shortcuts for translation, as detailed in [Actions](#translate-action).

#### 3. Replace with Translation :id=usage-translate-and-replace

Translate the target text and replace it.
If the target language is English, the output has several formats: `in the camel case`,
`with a word separator` (when the output contains multiple words) and `in the original format`.

?> *Instructions:* Select a text or hover the mouse over the text > <kbd>**Right-click**</kbd> >
<kbd>**Replace with Translation**</kbd> (editor only, please use shortcuts for Input box,
as detailed in [Actions](#translate-and-replace-action)).

_Editor:_

![Editor: Replace with Translation](/img/translation_replacement.gif ':size=400x380')

_Input box:_

![Input box: Replace with Translation](/img/translation_replacement_component.gif ':size=460x400')

_Select language before translation:_

![Select language before translation](/img/language_selection.gif ':size=680x620')

?> _Enable right-click menu option:_ <kbd>**Translation Settings**</kbd> > <kbd>**Replace with Translation**</kbd> >
Check the <kbd>**Add to context menu**</kbd> option.  
_Enable "Select language before translation":_ <kbd>**Translation Settings**</kbd> >
<kbd>**Replace with Translation**</kbd> > Check the <kbd>**Select language before translation**</kbd> option.  
_Separator configuration:_ <kbd>**Translation Settings**</kbd> > <kbd>**Replace with Translation**</kbd> >
<kbd>**Separators**</kbd>.

#### 4. Translate documentation :id=usage-translate-doc

- Right-click within a documentation view (including editor inline documentation rendered view)
  or within a documentation comment block > <kbd>**Translate Documentation**</kbd>
  to toggle the translation status of the documentation. Or use the shortcut
  <kbd>**Ctrl + Shift + Q**</kbd> (macOS: <kbd>**Control + Q**</kbd>).
- When the "**Automatically translate documentation**" option is enabled,
  the documentation will be automatically translated when you view the Quick Documentation.

_Quick documentation:_

![Quick documentation](/img/docs_translation.gif?v1.0 ':size=580x380')

_Keyboard shortcuts:_

![Documentation translation with shortcuts](/img/docs_translation2.gif ':size=680x520')

_Editor inline documentation rendered view:_

![Editor inline documentation rendered view](/img/docs_inline_doc_translation.gif ':size=500x350')

_Batch translate editor inline documentation view:_

![Batch translate inline documentation view](/img/batch-inline-doc-translation.gif ':size=550x350')

?> _Enable the "**Automatically translate documentation**" option:_ <kbd>**Translation Settings**</kbd> >
<kbd>**Other**</kbd> > <kbd>**Automatically translate documentation**</kbd>.

!> *Note:* Editor inline documentation does not support automatic translation.

#### 5. Switch engines :id=usage-switch-engines

Click the engine widget in the status bar or use the shortcut <kbd>**Ctrl + Shift + S**</kbd>
(macOS: <kbd>**Control + Meta + Y**</kbd>) to switch between the translation engine and the TTS engine quickly.

![Translation engines](/en/img/translation_engines.png ':size=233x314')

[gh:releases]: https://github.com/YiiGuxing/TranslationPlugin/releases
[plugin:versions]: https://plugins.jetbrains.com/plugin/8579-translation/versions
[deepl]: https://www.deepl.com
[youdao-cloud]: https://ai.youdao.com
[baidu-dev]: https://fanyi-api.baidu.com/manage/developer
[ali-mt]: https://www.aliyun.com/product/ai/base_alimt


# Actions :id=actions

#### 1. Show Translation Dialog... :id=show-trans-dialog-action

Open the translation dialog, which appears by default on the toolbar. Default shortcut:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![Translation dialog](/img/translation_dialog.png ':size=550x250')

#### 2. Translate :id=translate-action

Extract words and translate them.
If you have already selected a text, extract the words from the portion of the text you'd like to translate.
Otherwise, words are extracted automatically from the maximum range
(this extraction can be configured in **Translation Settings**).
This action is displayed by default in the editor's right-click context menu.
Default shortcut:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![Translate Action](/img/translate_auto_selection.gif ':size=300x250')

#### 3. Translate (inclusive) :id=translate-inclusive-action

Extract words and translate them.
Automatically extract and translate all words from a specific range, ignoring manually selected text.
Default shortcut: (None)

![Translate (inclusive) Action](/img/translate_inclusive.gif ':size=300x250')

#### 4. Translate (exclusive) :id=translate-exclusive-action

Extract words and translate them.
Automatically extract the nearest single word, ignoring manually selected text.
Default shortcut: (None)

![Translate (exclusive) Action](/img/translate_exclusive.gif ':size=300x250')

#### 5. Replace with Translation :id=translate-and-replace-action

Replace the text with translation.
Available in editors and input boxes,
the word extraction method works the same as the [Translate Action](#translate-action).
Default shortcut:
- Windows - <kbd>**Ctrl + Shift + X**</kbd>
- macOS - <kbd>**Control + Meta + O**</kbd>

_Editor:_

![Editor: Replace with Translation](/img/translation_replacement_by_shortcut.gif ':size=260x380')

_Input box:_

![Input box: Replace with Translation](/img/translation_replacement_component.gif ':size=460x400')

_Select language before translation:_

![Select language before translation](/img/language_selection.gif ':size=680x620')

?> _Tip:_ The "Select language before translation" feature is disabled by default. To enable it, go to:
<kbd>**Translation Settings**</kbd> > <kbd>**Replace with Translation**</kbd> > Check <kbd>**Select language before translation**</kbd>.

#### 6. Translate Documentation :id=translate-doc-action
##### 6.1. Translate Quick Documentation :id=translated-quick-documentation-action

Quickly display and translate the documentation of the symbol at the caret. When used in the
Quick Documentation popup, you can toggle between the translated text and the original.
Default shortcut:
- Windows - <kbd>**Ctrl + Shift + Q**</kbd>
- macOS - <kbd>**Control + Q**</kbd>

![Documentation translation](/img/docs_translation.gif?v1.0 ':size=580x380')

_Keyboard shortcuts:_

![Documentation translation with shortcuts](/img/docs_translation2.gif ':size=680x520')

##### 6.2. Translate Inline Documentation :id=translate-inline-doc-action

Translate the contents of the editor's inline documentation rendered view.
Displayed by default in the context menu of the inline documentation view
and available within the inline documentation view. Default shortcut: (None)

![Editor inline documentation rendered view](/img/docs_inline_doc_translation.gif ':size=500x350')

##### 6.3. Batch Translate Inline Documentation :id=batch-inline-doc-translation

Batch translate all inline documentation in the editor. Displayed by default on the
editor inspections widget toolbar and available in Reader mode. Default shortcut: (None)

![Batch translate inline documentation view](/img/batch-inline-doc-translation.gif ':size=550x350')

#### 7. Translate the Text Component :id=translate-text-component-action

Translate selected text in some text components (such as Quick Docs, popup hints, input boxes, etc.).
This action does not support automatic word extraction.
Default shortcut:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

#### 8. Switch Engine :id=switch-engine-action

Quickly switch between translation engine and TTS engine. Default shortcut:
- Windows - <kbd>**Ctrl + Shift + S**</kbd>
- macOS - <kbd>**Control + Meta + Y**</kbd>

![Translation Engines](/en/img/translation_engines.png ':size=233x314')

#### 9. Word of the Day :id=word-of-the-day-action

Display the "**Word of the Day**" dialog. Default shortcut: (None)

![Word of the Day](/en/img/word_of_the_day.png ':size=552x478 :class=round')

#### 10. Other :id=other-actions

- **Translation dialog shortcuts:**
  - Display the list of source languages - <kbd>**Alt + S**</kbd>
  - Display the list of target languages - <kbd>**Alt + T**</kbd>
  - Switch between languages - <kbd>**Alt + Shift + S**</kbd>
  - Pin/unpin a window - <kbd>**Alt + P**</kbd>
  - Play TTS - <kbd>**Alt/Meta/Shift + Enter**</kbd>
  - Save to Word Book - <kbd>**Ctrl/Meta + F**</kbd>
  - Show history - <kbd>**Ctrl/Meta + H**</kbd>
  - Copy translation - <kbd>**Ctrl/Meta + Shift + C**</kbd>
  - Clear input - <kbd>**Ctrl/Meta + Shift + BackSpace/Delete**</kbd>
  - Expand more translations - <kbd>**Ctrl/Meta + Down**</kbd>
  - Hide more translations - <kbd>**Ctrl/Meta + UP**</kbd>
- **Translation balloon shortcuts:**
  - Open dialog - <kbd>**Ctrl + Shift + Y**</kbd> / <kbd>**Control + Meta + U**</kbd>
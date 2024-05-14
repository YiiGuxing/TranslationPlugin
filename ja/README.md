# はじめに :id=introduction

TranslationPlugin is an IntelliJ-based IDEs/Android Studio Translation Plugin. It integrates many translation engines such as Google Translate, Microsoft Translate, DeepL Translate, etc. You can translate the text, code comments, and code documents, etc. You want to translate in your IDE at any time.

![TranslationPlugin](../img/translation_plugin.png)

## 特徴 :id=features

- マルチ翻訳エンジン
    - Google 翻訳
    - Microsoft 翻訳
    - OpenAI 翻訳
    - DeepL 翻訳
    - Youdao 翻訳
    - Baidu 翻訳
    - Alibaba 翻訳
- 多言語翻訳
- 音声読み上げ
  - Microsoft Edge TTS
  - Google TTS
  - OpenAI TTS
- ドキュメントの翻訳
- 単語の自動選択
- 単語の自動分割
- 単語帳

## コンパチビリティ :id=compatibility

サポートされている IDE 製品：
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


# クイックスタート :id=quick-start

## インストール :id=installation

<div class="button--plugin-installation">
  <iframe src="https://plugins.jetbrains.com/embeddable/install/8579" frameborder="none"></iframe>
</div>

**IDE組み込みプラグインシステムを使用してインストールします：**
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>**Marketplace**</kbd> > 「**Translation**」を検索 > プラグインをインストールします。
**手動インストール：**
- [GitHub][gh:releases] または [JetBrains Marketplace][plugin:versions]にアクセスして、最新のプラグインパッケージをダウンロードします。
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>⚙</kbd> > <kbd>**Install plugins from disk...**</kbd> > プラグインパッケージを選択してインストール（解凍不要）。

インストール後に IDE を再起動します。

## 使用 :id=usage

#### 1. 翻訳サービスへの登録（オプション） :id=usage-sing-up

多くの翻訳サービスは、利用するためにユーザー登録が必要です（例：OpenAI、DeepL、Youdao Translateなど）。そのため、アカウントを作成し、認証キーを取得し、プラグイン内で認証キーをバインドする必要があります：<kbd>**Preferences(Settings)**</kbd> > <kbd>**Tools**</kbd> > <kbd>
  **Translation**</kbd> > <kbd>**全般**</kbd> > <kbd>**翻訳エンジン**</kbd> > <kbd>**構成...**</kbd>

#### 2. 翻訳の開始 :id=usage-start-translating

テキストを選択するか、マウスをテキストに向けて、マウスの右ボタンをクリックして、翻訳します。

![](../img/translate.gif ':size=520x450')

?> または、ショートカットキーを使用して翻訳します。詳細については、「[アクション](#translate-action)」を参照してください。

#### 3. 翻訳と置き換え :id=usage-translate-and-replace

ターゲット言語を翻訳して置き換えます。翻訳先言語が英語の場合、`アストラ式の翻訳結果`、`単語区切り付きの翻訳結果`（複数の単語を含む場合、単語の区切り符号はプラグインの設定画面で設定可能：翻訳設定 > 区切り記号）、そして`元の翻訳結果`をそれぞれ出力します。

?> 使用法：テキストを選択するか、マウスがテキストを向けて > マウスの右ボタンをクリック > 翻訳と置換...（または、ショートカットキーを使用して翻訳します。詳細については、「[アクション](#translate-action)」を参照してください）。

_エディタ：_

![エディタ](../img/translation_replacement.gif ':size=400x380')

_入力ボックス：_

![入力ボックス](../img/translation_replacement_component.gif ':size=460x400')

?> _Enable right-click menu option:_ <kbd>**Translation Settings**</kbd> > <kbd>**Translate and Replace**</kbd> > Enables the <kbd>**Add to context menu**</kbd> option.  
_Separator configuration:_ <kbd>**Translation Settings**</kbd> > <kbd>**Translate and Replace**</kbd> > <kbd>**Separators**</kbd>.

#### 4. ドキュメントの翻訳 :id=usage-translate-doc

- Right-click within a documentation view (including editor inlay documentation rendered view) or within a documentation comment block > <kbd>**ドキュメントの翻訳**</kbd> (or click the Translate Documentation icon on the documentation view toolbar) to toggle the translation status of the documentation.
- When the "**Automatically translate documentation**" option is enabled, the documentation will be automatically translated when you view the Quick Documentation.

_Quick documentation:_

![Quick documentation](../img/docs_translation.gif ':size=302x162 :class=round')

_Documentation comment:_

![Documentation comment](../img/doc_comment_translation.gif ':size=400x380')

_Editor inlay documentation rendered view:_

![Editor inlay documentation rendered view](../img/docs_inlay_comment_translation.gif ':size=400x300')

?> _Enable the "**Automatically translate documentation**" option:_ <kbd>**Translation Settings**</kbd> > <kbd>**Other**</kbd> > <kbd>**Automatically translate documentation**</kbd>.

!> *Note:* Editor inlay documentation do not support automatic translation.

#### 5. 翻訳エンジンの切り替え :id=usage-switch-translation-engine

ステータスバーの翻訳エンジンステータスアイコンをクリックするか、ショートカットキーの <kbd>**Ctrl + Shift + S**</kbd>（macOS: <kbd>**Control + Meta + Y**</kbd>）を使用して、翻訳エンジンをすばやく切り替えることができます。

![翻訳エンジン](../ja/img/translation_engines.png ':size=218x259')

[gh:releases]: https://github.com/YiiGuxing/TranslationPlugin/releases
[plugin:versions]: https://plugins.jetbrains.com/plugin/8579-translation/versions
[deepl]: https://www.deepl.com
[youdao-cloud]: https://ai.youdao.com
[baidu-dev]: https://fanyi-api.baidu.com/manage/developer
[ali-mt]: https://www.aliyun.com/product/ai/base_alimt


# アクション :id=actions

#### 1. 翻訳ダイアログを表示... :id=show-trans-dialog-action

翻訳ダイアログを開きます。デフォルトでは、ツールバーに表示されます。デフォルトのショートカットキー：
- Windows - <kbd>**Ctrl + Shift + O**</kbd>
- macOS - <kbd>**Control + Meta + I**</kbd>

![翻訳ダイアログを](../img/translation_dialog.png ':size=550x250')

#### 2. 翻訳 :id=translate-action

単語を取得して翻訳します。すでにテキストが選択されている場合には、まず選択されたテキストから単語を取得します。選択されているテキストがない場合は、デフォルトでテキストの最大範囲から自動的に単語を取得します。（単語の取得モードは Settings で設定可能です）デフォルトはエディタの右クリックメニューに表示され、デフォルトのショートカットキーは次のとおりです：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![翻訳](../img/translate_auto_selection.gif ':size=300x250')

#### 3. 翻訳（すべて） :id=translate-inclusive-action

単語を取得して翻訳します。テキストの最大範囲から自動的にすべての単語を取得します。手動で選択されたテキストを無視します。デフォルトのショートカットキー：（なし）

![翻訳（すべて）](../img/translate_inclusive.gif ':size=300x250')

#### 4. 翻訳（単独） :id=translate-exclusive-action

単語を取得して翻訳します。最も近い単語を自動的に取得します。手動で選択されたテキストを無視します。デフォルトのショートカットキー：（なし）

![翻訳（単独）](../img/translate_exclusive.gif ':size=300x250')

#### 5. 翻訳と置換... :id=translate-and-replace-action

翻訳して置き換えます。単語を取得すると同時に翻訳を行います。デフォルトのショートカットキー：
- Windows - <kbd>**Ctrl + Shift + X**</kbd>
- macOS - <kbd>**Control + Meta + O**</kbd>

_エディタ：_

![エディタ](../img/translation_replacement_by_shortcut.gif ':size=260x380')

_入力ボックス：_

![入力ボックス](../img/translation_replacement_component.gif ':size=460x400')

#### 6. ドキュメントの翻訳 :id=translate-doc-action
##### 6.1. クイックドキュメント翻訳に切り替える :id=toggle-quick-doc-translation-action

クイックドキュメント中のテキスト表示を翻訳文と原文に切り替えます。クイックドキュメントのポップアップまたはドキュメントツールのウィンドウを選択している場合に使用できます。デフォルトのショートカットキー（翻訳と同じ）：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![Documentation translation](../img/docs_translation.gif ':size=302x162 :class=round')

##### 6.2. Translate Documentation Comment :id=translate-doc-comment-action

Translate documentation comment content. Appears on the editor right-click context menu by default, and is available when inside a documentation comment block. Default shortcut: (None)

_Documentation comment:_

![Documentation comment](../img/doc_comment_translation.gif ':size=400x380')


_Editor inlay documentation rendered view:_

![Editor inlay documentation rendered view:](../img/docs_inlay_comment_translation.gif ':size=400x300')

#### 7. テキストコンポーネントの翻訳 :id=translate-text-component-action

テキストコンポーネント（クイックドキュメント、メッセージバブル、入力フィールドなど）で選択されたテキストを翻訳します。単語の自動取得をサポートしません。デフォルトのショートカットキー：
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

#### 8. 翻訳エンジンの選択 :id=switch-translation-engine-action

翻訳エンジンを素早く切り替えます。デフォルトのショートカットキー：
- Windows - <kbd>**Ctrl + Shift + S**</kbd>
- macOS - <kbd>**Control + Meta + Y**</kbd>

![翻訳エンジン](../ja/img/translation_engines.png ':size=218x259')

#### 9. 今日の単語 :id=word-of-the-day-action

[今日の単語] ダイアログを表示します。デフォルトのショートカットキー：（なし）

![今日の単語](../ja/img/word_of_the_day.png ':size=552x478 :class=round')

#### 10. その他 :id=other-actions

- **翻訳ダイアログのショートカット：**
  - ソース言語の一覧を表示 - <kbd>**Alt + S**</kbd>
  - ターゲット言語の一覧を表示 - <kbd>**Alt + T**</kbd>
  - 言語を変更 - <kbd>**Alt + Shift + S**</kbd>
  - ウィンドウの固定状態を切り替える - <kbd>**Alt + P**</kbd>
  - TTS を再生 - <kbd>**Alt/Meta/Shift + Enter**</kbd>
  - 単語帳にお気に入り - <kbd>**Ctrl/Meta + F**</kbd>
  - 履歴の表示 - <kbd>**Ctrl/Meta + H**</kbd>
  - 翻訳のコピー - <kbd>**Ctrl/Meta + Shift + C**</kbd>
  - 入力を空にする - <kbd>**Ctrl/Meta + Shift + BackSpace/Delete**</kbd>
  - さらに多くの翻訳を表示 - <kbd>**Ctrl/Meta + Down**</kbd>
  - さらに多くの翻訳を非表示 - <kbd>**Ctrl/Meta + UP**</kbd>
- **翻訳バブルのショートカットキー：**
  - ダイアログで開く - <kbd>**Ctrl + Shift + Y**</kbd> / <kbd>**Control + Meta + U**</kbd>
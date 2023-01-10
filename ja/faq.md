# よくある質問 :id=faq

ここでは、**TranslationPlugin** を使用する過程で発生する可能性のある問題と解決策をいくつか示します。

ここに答えがない場合は、[GitHubで過去に報告された問題][gh:issues]を確認するか、[GitHubのディスカッション][gh:discussions]で助けを求めてください。

[gh:issues]: https://github.com/YiiGuxing/TranslationPlugin/issues
[gh:discussions]: https://github.com/YiiGuxing/TranslationPlugin/discussions


## ネットワークエラーやネットワーク接続のタイムアウトが発生した場合はどうすればいいですか？ :id=faq-network-error

**TranslationPlugin** は現在、すべての翻訳操作をオンラインで翻訳しており、現時点ではオフライン翻訳をサポートしていません。したがって、翻訳操作を実行する前に、マシンが良好なネットワーク環境にあることを確認してください。翻訳中にネットワーク エラーやネットワーク接続のタイムアウトなどのネットワークの問題が発生した場合は、次のようにネットワーク接続を確認してください：
- ネットワーク環境を確認して、ネットワークが開いていることを確認します。
- プロキシソフトウェアを使用してプラグインが翻訳 API にアクセスできないかどうかを確認します。
- IDE プロキシの設定に問題があるかどうかを確認します。

## アプリケーションキーを保存できない場合はどうすればいいですか？ :id=faq-save-key

パスワードの保存方法を `In KeePass` 方式に変更してみてください（<kbd>**Preferences(Settings)**</kbd> > <kbd>**Appearance & Behavior**</kbd> > <kbd>**System Settings**</kbd> > <kbd>**Passwords**</kbd>）。原因と詳細：
- macOS の場合は、[#81](https://github.com/YiiGuxing/TranslationPlugin/issues/81)
- Linux の場合は、[#115](https://github.com/YiiGuxing/TranslationPlugin/issues/115)

## 翻訳された内容が文字化けしている場合はどうすればいいですか？ :id=faq-garbled

文字化けは通常、フォントに対応する文字がないことが原因です。プラグインの設定ページでフォントを変更して文字化けの問題を解決できます（次の図を参照）。

![](../ja/img/settings_font.png ':class=round')

## ショートカットキーが使用できない場合はどうすればいいですか？ :id=faq-shortcuts

ショートカットキーは他のプラグインまたは外部アプリケーションによって使用されている場合、動作しません。該当する操作のために新しいショートカットキーをリセットできます。
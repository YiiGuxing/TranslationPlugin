# 常见问题及解答 :id=faq

这里我们列出了一些使用 **TranslationPlugin** 的过程中可能遇到的问题及解决方案。

如果你在这里没有找到你的问题的答案，请查看 [GitHub Issues][gh:issues] 上已报告的问题，或者在 [GitHub 讨论][gh:discussions] 上寻求帮助。

[gh:issues]: https://github.com/YiiGuxing/TranslationPlugin/issues
[gh:discussions]: https://github.com/YiiGuxing/TranslationPlugin/discussions


## 为什么我不能使用 Google 翻译和 Google TTS？

2022年10月1日，Google 突然停止了 Google 翻译在中国大陆的业务，不再向中国大陆区域提供翻译服务，官方给出的理由是“因为使用率低”。这一变化直接不可避免地影响到了插件内置的 Google 翻译引擎和基于 Google 翻译的语音朗读（TTS）功能，导致其无法正常使用。因此建议大家换用其他翻译引擎，微软翻译、有道翻译、百度翻译和阿里翻译都是很不错的选择。未来开发者会带来更多新的翻译引擎和语音合成（TTS）引擎供大家选择，敬请期待！

当然，如果你仍希望能够继续使用 Google 翻译和 Google TTS，也并不是没有办法，详情请参考《[关于Google翻译和语音朗读功能无法正常使用的说明](https://github.com/YiiGuxing/TranslationPlugin/discussions/2315)》。

## 翻译时出现网络错误或者网络连接超时怎么办？ :id=faq-network-error

**TranslationPlugin** 目前所有翻译操作都是在线翻译，暂时还不支持离线翻译。因此在进行翻译操作之前，请确保你的机器正处于一个良好的网络环境。如果你在翻译时出现网络错误或者网络连接超时等网络问题，请按以下操作检查你的网络连接：
- 检查网络环境，确保网络畅通；
- 检查是否是因为使用了代理软件而导致插件无法访问翻译服务器；
- 检查 **IDE** 代理配置，查看是否是因为 **IDE** 代理配置导致的问题。

## 无法保存翻译引擎的认证密钥怎么办？ :id=faq-save-key

可以尝试将密码保存方式改成 `In KeePass` 的方式 (<kbd>**Preferences(Settings)**</kbd> > <kbd>**Appearance & Behavior**</kbd> > <kbd>**System Settings**</kbd> > <kbd>**Passwords**</kbd>)，原因与细节：
- macOS，请另阅 [#81](https://github.com/YiiGuxing/TranslationPlugin/issues/81)
- Linux，请另阅 [#115](https://github.com/YiiGuxing/TranslationPlugin/issues/115)

## 翻译内容出现乱码怎么办？ :id=faq-garbled

由于一些翻译引擎支持翻译到众多的目标语言，如果**IDE**当前使用的字体中没有这些语言所需的字体，此时就很有可能会出现乱码，这时你可以到插件的设置页面修改字体以解决乱码问题（如下图所示）。

![](img/settings_font.png ':class=round')

## 快捷键不能使用怎么办? :id=faq-shortcuts

快捷键不能使用可能是因为被其他插件或者外部应用占用了，可以为相应的操作重新设置新的快捷键。
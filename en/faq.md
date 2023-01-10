# FAQ :id=faq

Here we list some problems and solutions that may be encountered in the process of using **TranslationPlugin**.

If you don't see an answer to your question here, check our previously [reported issues on GitHub][gh:issues], or ask for help in [GitHub discussions][gh:discussions].

[gh:issues]: https://github.com/YiiGuxing/TranslationPlugin/issues
[gh:discussions]: https://github.com/YiiGuxing/TranslationPlugin/discussions


## What should I do if there is a network error or the network connection times out? :id=faq-network-error

**TranslationPlugin** currently translates all translation operations online and does not support offline translation at the moment. Therefore, please make sure your machine is in a good network environment before performing translation operations. If you have network problems such as network error or network connection timeout while translating, please check your network connection as follows:
- Check the network environment and make sure the network is running smoothly.
- Check whether a proxy is preventing the plugin from accessing the translation API.
- Check the IDE proxy configuration to see if that is the cause of the problem.

## What if I can't save the application key? :id=faq-save-key

You can try changing the way passwords are saved to `In KeePass` (<kbd>**Preferences(Settings)**</kbd> > <kbd>**Appearance & Behavior**</kbd> > <kbd>**System Settings**</kbd> > <kbd>**Passwords**</kbd>). 

![](../img/ide_passwords.png ':class=round')

For more details:
- For macOS, please refer to [#81](https://github.com/YiiGuxing/TranslationPlugin/issues/81)
- For Linux, please refer to [#115](https://github.com/YiiGuxing/TranslationPlugin/issues/115)

## What should I do if the translated content appears garbled? :id=faq-garbled

The Garbled code generally appears when there is a lack of corresponding characters in the font. You can go to the Settings page of the plugin to modify the font in order to fix the garbled code (as shown below).

![](img/settings_font.png ':class=round')

## What if the shortcuts don't work? :id=faq-shortcuts

The shortcut keys are most likely not working because they are being used in other plugins or external applications. You can reset shortcut keys for the corresponding operations.
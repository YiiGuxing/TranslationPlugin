# 자주 묻는 질문 :id=faq

Here we list some problems and solutions that may be encountered in the process of using **IntelliJ Translation Plugin**.

If you don't see an answer to your question here, check our previously [reported issues on GitHub][gh:issues], or ask for help in [GitHub discussions][gh:discussions].

[gh:issues]: https://github.com/YiiGuxing/TranslationPlugin/issues
[gh:discussions]: https://github.com/YiiGuxing/TranslationPlugin/discussions


## 네트워크 오류 또는 네트워크 연결 시간이 초과되면 어떻게 해야 하나요? :id=faq-network-error

**TranslationPlugin**은 현재 모든 번역 작업을 온라인으로 번역하며 현재 오프라인 번역을 지원하지 않습니다. 따라서 번역 작업을 수행하기 전에 컴퓨터가 양호한 네트워크 환경에 있는지 확인하십시오. 번역 중 네트워크 오류나 네트워크 연결 시간 초과 등의 네트워크 문제가 발생하면 다음과 같이 네트워크 연결을 확인하세요.
- 네트워크 연결이 원활한지 네트워크 환경을 확인하세요.
- 프록시가 플러그인이 번역 API에 액세스하는 것을 차단하고 있는지 확인하세요.
- IDE 프록시 구성을 확인하여 IDE 프록시 구성으로 인해 문제가 발생했는지 확인하세요.

## 앱 키를 저장할 수 없는 경우 어떻게 해야 하나요? :id=faq-save-key

암호 저장 방법을 `In KeePass` 방법으로 변경해 보세요 (<kbd>**Preferences(Settings)**</kbd> > <kbd>**Appearance & Behavior**</kbd> > <kbd>**System Settings**</kbd> > <kbd>**Passwords**</kbd>).

![](../img/ide_passwords.png ':class=round')

원인 및 세부 정보는 다음과 같습니다:
- macOS의 경우, [#81](https://github.com/YiiGuxing/TranslationPlugin/issues/81)을 참조하세요.
- Linux의 경우, [#115](https://github.com/YiiGuxing/TranslationPlugin/issues/115)를 참조하세요.

## 번역 내용에 깨진 글자가 나타나면 어떻게 하나요? :id=faq-garbled

깨진 글자는 일반적으로 글꼴에 해당 문자가 없기 때문에 발생하는 문제이며, 플러그인의 설정 페이지에서 글꼴을 수정하여 깨진 글자 문제를 해결할 수 있습니다(아래 그림 참조).

![](../en/img/settings_font.png ':class=round')

## 단축키를 사용할 수 없는 경우 어떻게 해야 하나요? :id=faq-shortcuts

다른 플러그인이나 외부 응용 프로그램에 의해 해당 단축키가 사용 중인 경우에는 사용할 수 없으며, 이때는 해당 작업에 대해 새로운 단축키를 설정하면 됩니다.
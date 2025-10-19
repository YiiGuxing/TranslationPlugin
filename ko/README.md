# 소개 :id=introduction

TranslationPlugin은 IntelliJ 기반의 IDEs/Android Studio 번역 플러그인입니다. Google 번역, Microsoft 번역, OpenAI 번역, DeepL 번역 등 많은 번역 엔진을 통합합니다. 텍스트, 코드 주석, 코드 문서 등을 IDE에서 언제든지 번역할 수 있습니다.

![TranslationPlugin](/img/translation_plugin.png)

## 특성 :id=features

- 여러 번역 엔진
  - Google 번역
  - Microsoft 번역
  - DeepL 번역
  - Youdao 번역
  - Baidu 번역
  - Alibaba 번역
  - OpenAI 번역, 호환성:
    - DeepSeek
    - Doubao
    - Gemini
    - Kimi
    - Ollama
    - Qwen
    - ...
- 다국어 번역
- 음성 읽기
  - Microsoft Edge TTS
  - Google TTS
  - OpenAI TTS
- 문서 번역
- 텍스트 번역 및 바꾸기
- 자동 단어 선택
- 자동 단어 분할
- 단어장

## 호환성 :id=compatibility

지원하는 IDE 제품::
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


# 빠른 시작 :id=quick-start

## 설치 :id=installation

<div class="button--plugin-installation">
  <iframe src="https://plugins.jetbrains.com/embeddable/install/8579" frameborder="none"></iframe>
</div>

**IDE 내의 플러그인 저장소에서 바로 다운로드하여 설치:**
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>**Marketplace**</kbd> > "**Translation**" 검색 > 플러그인 설치.
**수동 설치:**
- [GitHub Releases][gh:releases] 또는 [JetBrains Marketplace][plugin:versions] 에서 최신 플러그인 패키지 다운로드.
- <kbd>**Preferences(Settings)**</kbd> > <kbd>**Plugins**</kbd> > <kbd>⚙</kbd> > <kbd>**Install plugins from disk...**</kbd> > 패키지 선택 및 설치(압축 해제 필요 없음).

설치 후 IDE를 다시 시작합니다.

## 사용 :id=usage

#### 1. 번역 서비스 가입 (선택 사항) :id=usage-sing-up

대부분의 번역 서비스는 서비스 이용을 위해 사용자 등록이 필요합니다 (예: OpenAI, DeepL, Youdao Translate 등). 따라서 계정을 생성하고 인증 키를 획득한 후 플러그인 내에서 인증 키를 바인딩해야 할 수도 있습니다: <kbd>**Preferences(Settings)**</kbd> > <kbd>**Tools**</kbd> > <kbd>
  **Translation**</kbd> > <kbd>**일반**</kbd> > <kbd>**번역 엔진**</kbd> > <kbd>**구성...**</kbd>

#### 2. 번역 시작 :id=usage-start-translating

텍스트를 선택하거나 마우스로 텍스트를 가리킴 > 마우스 오른쪽 버튼 클릭 > <kbd>**Translate**</kbd>.

![번역 시작](/img/translate.gif ':size=520x450')

?> 또는 단축키를 사용하여 번역, 자세한 내용은 [액션](#translate-action) 참고.

#### 3. 번역 및 바꾸기 :id=usage-translate-and-replace

대상 텍스트를 번역하고 바꿉니다. 번역 대상 언어가 영어인 경우, 출력 결과의 형식은 다음과 같습니다: `낙타 표기법`, `단어 구분 기호 포함` (출력 결과에 여러 단어가 포함된 경우) 및 `원래 형식`.

?> *사용법:* 텍스트 선택 또는 마우스로 텍스트 가리킴 > 마우스 오른쪽 클릭 > <kbd>**번역 및 바꾸기...**</kbd> (에디터만 해당, 입력 상자는 단축키 사용, 자세한 내용은 [액션](#translate-and-replace-action) 참고).

_편집기:_

![편집기: 번역 및 바꾸기](/img/translation_replacement.gif ':size=400x380')

_입력 상자:_

![입력 상자: 번역 및 바꾸기](/img/translation_replacement_component.gif ':size=460x400')

_번역 전에 언어 선택:_

![번역 전에 언어 선택](/img/language_selection.gif ':size=680x620')

?> _마우스 오른쪽 메뉴 옵션 활성화:_ <kbd>**번역 설정**</kbd> > <kbd>**번역 및 바꾸기**</kbd> > <kbd>**컨텍스트 메뉴에 추가**</kbd> 옵션 활성화.  
_"번역 전에 언어 선택" 활성화:_ <kbd>**번역 설정**</kbd> > <kbd>**번역 및 바꾸기**</kbd> > <kbd>**번역 전에 언어 선택**</kbd> 체크.  
_구분 기호 설정:_ <kbd>**번역 설정**</kbd> > <kbd>**번역 및 바꾸기**</kbd> > <kbd>**구분 기호**</kbd>.

#### 4. 문서 번역 :id=usage-translate-doc

- 문서 보기(에디터 인레이 문서 렌더링 뷰 포함) 또는 문서 주석 블록 내에서 마우스 오른쪽 버튼 클릭 > <kbd>**문서 번역**</kbd> 으로 문서의 번역 상태를 전환할 수 있습니다. 또는 단축키 <kbd>**Ctrl + Shift + Q**</kbd> (macOS: <kbd>**Control + Q**</kbd>)를 사용합니다.
- "**문서 자동 번역**" 옵션을 활성화하면 빠른 문서 보기 시 문서가 자동으로 번역됩니다.

_빠른 문서:_

![빠른 문서](/img/docs_translation.gif?v1.0 ':size=580x380')

_단축키 조작:_

![단축키로 문서 번역](/img/docs_translation2.gif ':size=680x520')

_에디터 인레이 문서 렌더링 뷰:_

![에디터 인레이 문서 렌더링 뷰](/img/docs_inline_doc_translation.gif ':size=500x350')

_인라인 문서 보기 일괄 번역:_

![인라인 문서 보기 일괄 번역](/img/batch-inline-doc-translation.gif ':size=550x350')

?> _"**문서 자동 번역**" 옵션 활성화:_ <kbd>**번역 설정**</kbd> > <kbd>**기타**</kbd> > <kbd>**문서 자동 번역**</kbd>.

!> *참고:* 에디터 인레이 문서는 자동 번역을 지원하지 않습니다.

#### 5. 엔진 전환 :id=usage-switch-engines

상태 표시줄의 엔진 위젯을 클릭하거나 단축키 <kbd>**Ctrl + Shift + S**</kbd> (macOS: <kbd>**Control + Meta + Y**</kbd>)를 사용하여 번역 엔진과 TTS 엔진을 빠르게 전환합니다.

![번역 엔진 전환](/en/img/translation_engines.png ':size=233x314 :class=round')

[gh:releases]: https://github.com/YiiGuxing/TranslationPlugin/releases
[plugin:versions]: https://plugins.jetbrains.com/plugin/8579-translation/versions
[deepl]: https://www.deepl.com
[youdao-cloud]: https://ai.youdao.com
[baidu-dev]: https://fanyi-api.baidu.com/manage/developer
[ali-mt]: https://www.aliyun.com/product/ai/base_alimt

# 액션 :id=actions

#### 1. 번역 대화상자 표시... :id=show-trans-dialog-action

번역 대화상자를 엽니다. 기본적으로 도구 모음에 표시됩니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![번역 대화상자](/img/translation_dialog.png ':size=550x250')

#### 2. 번역 :id=translate-action

단어를 가져와 번역합니다. 선택한 텍스트가 있으면 해당 텍스트에서 우선적으로 단어를 가져오고, 없으면 최대 범위에서 자동으로 단어를 가져옵니다(단어 가져오기 모드는 설정에서 변경 가능). 이 액션은 기본적으로 에디터에서 마우스 오른쪽 클릭 시 나타나는 컨텍스트 메뉴에 표시됩니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

![번역](/img/translate_auto_selection.gif ':size=300x250')

#### 3. 번역 (포괄적) :id=translate-inclusive-action

단어를 가져와 번역합니다. 가장 가까운 모든 단어를 최대 범위로 자동으로 가져오며, 수동 선택한 텍스트는 무시합니다. 기본 단축키: (없음)

![번역 (포괄적)](/img/translate_inclusive.gif ':size=300x250')

#### 4. 번역 (배타적) :id=translate-exclusive-action

단어를 가져와 번역합니다. 가장 가까운 개별 단어를 자동으로 가져오며, 수동으로 선택한 텍스트는 무시합니다. 기본 단축키: (없음)

![번역 (배타적)](/img/translate_exclusive.gif ':size=300x250')

#### 5. 번역 및 바꾸기... :id=translate-and-replace-action

텍스트를 번역하고 바꿉니다. 에디터와 입력 상자에서 사용할 수 있으며, 단어 추출 방식은 [번역](#translate-action)과 동일합니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + X**</kbd>
- macOS - <kbd>**Control + Meta + O**</kbd>

_편집기:_

![편집기: 번역 및 바꾸기](/img/translation_replacement_by_shortcut.gif ':size=260x380')

_입력 상자:_

![입력 상자: 번역 및 바꾸기](/img/translation_replacement_component.gif ':size=460x400')

_번역 전에 언어 선택:_

![번역 전에 언어 선택](/img/language_selection.gif ':size=680x620')

?> _팁:_ "번역 전에 언어 선택" 기능은 기본적으로 비활성화되어 있습니다. 활성화하려면 다음으로 이동하세요: <kbd>**번역 설정**</kbd> > <kbd>**번역 및 바꾸기**</kbd> > <kbd>**번역 전에 언어 선택**</kbd> 체크.

#### 6. 문서 번역 :id=translate-doc-action
##### 6.1. 빠른 문서 번역 :id=translated-quick-documentation-action

커서 위치 심볼의 문서를 빠르게 표시하고 번역합니다. 문서 팝업에서 사용하면 번역된 텍스트와 원문을 전환할 수 있습니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + Q**</kbd>
- macOS - <kbd>**Control + Q**</kbd>

![문서 번역](/img/docs_translation.gif?v1.0 ':size=580x380')

_단축키 조작:_

![단축키로 문서 번역](/img/docs_translation2.gif ':size=680x520')

##### 6.2. 인라인 문서 번역 :id=translate-inline-doc-action

에디터의 인라인 문서 렌더링 보기 내용을 번역합니다. 인라인 문서 보기의 컨텍스트 메뉴에 기본 표시되며, 인라인 문서 보기 내에서 사용할 수 있습니다. 기본 단축키: (없음)

![에디터 인레이 문서 렌더링 뷰](/img/docs_inline_doc_translation.gif ':size=500x350')

##### 6.3. 인라인 문서 일괄 번역 :id=batch-inline-doc-translation

에디터의 모든 인라인 문서를 일괄 번역합니다. 에디터 검사 위젯의 도구 모음에 기본 표시되며, 리더 모드에서 사용할 수 있습니다. 기본 단축키: (없음)

![인라인 문서 보기 일괄 번역](/img/batch-inline-doc-translation.gif ':size=550x350')

#### 7. 번역 텍스트 구성 요소 :id=translate-text-component-action

텍스트 구성 요소(빠른 문서, 알림 말풍선, 입력창 등)에서 선택한 텍스트를 번역합니다. 자동 단어 추출은 지원하지 않습니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + Y**</kbd>
- macOS - <kbd>**Control + Meta + U**</kbd>

#### 8. 엔진 전환 :id=switch-engine-action

번역 엔진과 TTS 엔진을 빠르게 전환합니다. 기본 단축키:
- Windows - <kbd>**Ctrl + Shift + S**</kbd>
- macOS - <kbd>**Control + Meta + Y**</kbd>

![번역기 선택](/en/img/translation_engines.png ':size=233x314 :class=round')

#### 9. 오늘의 단어 :id=word-of-the-day-action

오늘의 단어 대화상자를 표시합니다. 기본 단축키: (없음)

![오늘의 단어](/en/img/word_of_the_day.png ':size=552x478 :class=round')

#### 10. 기타 :id=other-actions

- **번역 대화 상자 단축키:**
  - 소스 언어 목록 표시 - <kbd>**Alt + S**</kbd>
  - 대상 언어 목록 표시 - <kbd>**Alt + T**</kbd>
  - 언어 전환 - <kbd>**Alt + Shift + S**</kbd>
  - 창을 고정/고정 해제 - <kbd>**Alt + P**</kbd>
  - TTS 재생 - <kbd>**Alt/Meta/Shift + Enter**</kbd>
  - 단어장에 저장 - <kbd>**Ctrl/Meta + F**</kbd>
  - 기록 표시 - <kbd>**Ctrl/Meta + H**</kbd>
  - 번역 복사 - <kbd>**Ctrl/Meta + Shift + C**</kbd>
  - 입력 내용 지우기 - <kbd>**Ctrl/Meta + Shift + BackSpace/Delete**</kbd>
  - 더 많은 번역 펼치기 - <kbd>**Ctrl/Meta + Down**</kbd>
  - 더 많은 번역 접기 - <kbd>**Ctrl/Meta + UP**</kbd>
- **번역 말풍선 단축키:**
  - 대화상자로 열기 - <kbd>**Ctrl + Shift + Y**</kbd> / <kbd>**Control + Meta + U**</kbd>
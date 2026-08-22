# OpenAI 번역 엔진에서 타사 AI 구성하기

[3.7](#/updates/v3.7 ':ignore :target=_blank') 버전에서 OpenAI 번역 엔진의 구성 가능성을 강화했습니다. API 엔드포인트와 AI 모델을 구성하여 OpenAI Chat Completions API와 호환되는 더 많은 타사 AI 서비스를 사용할 수 있습니다. 이 튜토리얼에서는 몇 가지 일반적인 타사 AI 서비스의 구성 방법을 소개합니다.

> 팁: 플러그인의 기본 프롬프트는 AI 서비스에 따라 동작이 다를 수 있습니다. 프롬프트 템플릿을 사용자 정의하여 조정하면 더 나은 번역 경험을 얻을 수 있습니다. 자세한 내용은 [OpenAI 번역 프롬프트 사용자 정의 방법](/ko/tutorial/how_to_customize_translation_prompts.md)을 참조하세요.

> 팁: 3.8.5 버전부터 요청 구성 파일을 통해 AI 서비스나 모델별로 요청 파라미터, 요청 헤더, 프롬프트 템플릿, 언어 매핑 등을 사용자 정의할 수 있습니다. 자세한 내용은 [사용자 정의 요청 구성](#custom-request-config)을 참조하세요.

## DeepSeek :id=deepseek

- **모델**: `deepseek-v4-flash`
- **API 엔드포인트**: `https://api.deepseek.com` `/v1/chat/completions`
- **API 키**: [API key](https://platform.deepseek.com/api_keys)

사고 모드 비활성화 구성 (플러그인 3.8.5 이상):

```json
{
  "$schema": "./openai-config.schema.json",
  "version": 1,
  "models": {
    "deepseek-v4-flash": {
      "request": {
        "body": {
          "thinking": {
            "type": "disabled"
          }
        }
      }
    }
  }
}
```
구성에 대한 설명은 [사용자 정의 요청 구성](#custom-request-config)을 참조하세요.

## Doubao :id=doubao

- **모델**: `deepseek-v3-241226`
- **API 엔드포인트**: `https://ark.cn-beijing.volces.com` `/api/v3/chat/completions`
- **API 키**: [API key](https://www.volcengine.com/docs/82379/1399008#b00dee71)

?> 다른 모델은 [모델 목록](https://www.volcengine.com/docs/82379/1330310)을 참조하세요.

## Gemini :id=gemini

- **모델**: `gemini-2.0-flash`
- **API 엔드포인트**: `https://generativelanguage.googleapis.com` `/v1beta/openai/chat/completions`
- **API 키**: [API key](https://ai.google.dev/gemini-api/docs/api-key?hl=ko)

?> 다른 모델은 [Gemini 모델](https://ai.google.dev/gemini-api/docs/models/gemini?hl=ko)을 참조하세요.

## Kimi :id=kimi

- **모델**: `moonshot-v1-8k`
- **API 엔드포인트**: `https://api.moonshot.cn` `/v1/chat/completions`
- **API 키**: [API key](https://platform.moonshot.cn/console/api-keys)

?> 다른 모델은 [모델 및 가격](https://platform.moonshot.cn/docs/pricing/chat)을 참조하세요 (참고: 현재 `kimi-latest` 모델은 지원되지 않습니다).

## Ollama :id=ollama

- **모델**: *\<실행 중인 모델, 예: `llama3.2:1b`>*
- **API 엔드포인트**: `http://localhost:11434` `/v1/chat/completions`
- **API 키**: *\<필수이지만 무시됨>*

?> 팁: API 키 설정 항목은 필수이지만 Ollama는 이를 무시하므로 비어 있지 않은 값을 입력하면 됩니다.

## Qwen :id=qwen

- **모델**: `qwen-max`
- **API 엔드포인트**: `https://dashscope.aliyuncs.com` `/compatible-mode/v1/chat/completions`
- **API 키**: [API key](https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key#ca06817d4cqro)

?> 다른 모델은 [모델 목록](https://help.aliyun.com/zh/model-studio/developer-reference/compatibility-of-openai-with-dashscope#7f9c78ae99pwz)을 참조하세요.

### Qwen-MT :id=qwen-mt

Qwen3 모델을 기반으로 최적화된 기계 번역 LLM인 Qwen-MT를 사용하려면 `qwen-mt-flash`를 예로 들어 다음 단계에 따라 구성하세요:

- 플러그인을 3.8.5 버전 이상으로 업그레이드합니다
- **OpenAI 번역 설정 대화상자**에서 Qwen-MT 모델을 `qwen-mt-flash`로 설정합니다
- 구성 디렉토리 (**OpenAI 번역 설정 대화상자** ⇒ <kbd>요청 설정 편집...</kbd> ⇒ <kbd>구성 디렉토리 열기</kbd>로 빠르게 열 수 있습니다)에 `prompts/qwen-mt.prompt` 파일을 만들고 내용을 다음과 같이 작성합니다:
```
$TEXT
```
- 요청 구성 파일을 편집합니다 (**OpenAI 번역 설정 대화상자** ⇒ <kbd>요청 설정 편집...</kbd>):
  ```json
  {
    "$schema": "./openai-config.schema.json",
    "version": 1,
    "models": {
      "qwen-mt-flash": {
        "languageMapping": {
          "zh-CN": "Chinese",
          "zh-TW": "Traditional Chinese"
        },
        "prompt": {
          "translator": "prompts/qwen-mt.prompt",
          "document": "prompts/qwen-mt.prompt"
        },
        "request": {
          "body": {
            "translation_options": {
              "source_lang": "${SOURCE_LANGUAGE}",
              "target_lang": "${TARGET_LANGUAGE}"
            }
          }
        }
      }
    }
  }
  ```

구성에 대한 설명은 [사용자 정의 요청 구성](#custom-request-config)을 참조하세요.

## 사용자 정의 요청 구성 :id=custom-request-config

다양한 벤더의 API는 OpenAI Chat Completions API와 호환되지만, 커스텀 파라미터(요청 본문 또는 요청 헤더에 있음)가 있을 수 있으며 모델마다 파라미터 요구 사항이 다를 수 있습니다. 3.8.5 버전부터 요청 구성 파일을 통해 AI 서비스나 모델별로 요청 파라미터, 요청 헤더, 프롬프트 템플릿, 언어 매핑 등을 사용자 정의할 수 있습니다.

!> 요청 구성은 번역(Chat Completions)에만 적용됩니다. TTS 및 공급자가 Azure인 경우에는 지원되지 않습니다.

!> 이것은 실험적인 기능이며 향후 버전에서 언제든지 폐기되거나 변경될 수 있습니다.

### 구성 파일

구성 파일은 플러그인의 `openai/config.json` 파일에 있습니다:

- Windows: `%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\openai\config.json`
- 기타 시스템: `$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/openai/config.json` 또는 `~/.TranslationPlugin/openai/config.json`

OpenAI 번역 설정 대화상자 왼쪽 하단의 **요청 설정 편집...** 버튼을 클릭하여 이 구성 파일을 편집할 수 있습니다. 대화상자에서 직접 편집할 때 IDE는 포함된 JSON Schema를 기반으로 검증과 자동 완성을 제공합니다. **주 편집기에서 열기** 버튼을 통해 IDE 주 편집기에서 편집할 수도 있으며(권장, 더 나은 경험을 제공하고 구성 필드 문서를 볼 수 있음), **구성 디렉토리 열기** 버튼을 통해 시스템 파일 관리자에서 구성 디렉토리를 열 수도 있습니다.

### 구성 예시

```json
{
  "$schema": "./openai-config.schema.json",
  "default": {
    "languageMapping": {
      "zh-CN": "Simplified Chinese",
      "zh-TW": "Traditional Chinese"
    },
    "prompt": {
      "translator": "prompts/translator.prompt",
      "document": "prompts/document.prompt"
    },
    "request": {
      "headers": {
        "X-Api-Version": "2024-01-01"
      },
      "body": {
        "temperature": 0.3,
        "top_p": 0.9
      }
    }
  },
  "models": {
    "gpt-5.4-mini": {
      "request": {
        "body": {
          "temperature": 0.7,
          "max_tokens": 4096
        }
      }
    }
  }
}
```

### Schema

Schema: [openai-config.schema.json](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/refs/tags/v3.8.5/src/main/resources/schemas/openai-config.schema.json)

에디터는 이 Schema를 기반으로 검증과 자동 완성을 수행할 수 있어야 합니다.

### 구성 설명

- `default`: 기본 구성으로 모든 모델이 공유합니다.
- `models`: 모델별 구성이며, 키는 UI에서 선택한 모델 ID(또는 사용자 정의 모델 이름)입니다. 모델 구성은 `default`와 깊은 병합(deep merge)되며 동일한 구성 항목은 덮어씁니다.
- `request`: 요청 사용자 정의.
    - `request.headers`: 추가 HTTP 요청 헤더.
    - `request.body`: 추가 요청 본문 필드(임의의 JSON 값).
        - `body.model`: 요청의 모델 ID를 덮어쓰는 데 사용할 수 있습니다. 예를 들어 동일한 오픈 소스 모델 `M1`이 A, B 두 벤더에 의해 동일한 모델 ID로 배포된 경우, UI에서 각각 `A/M1`과 `B/M1`을 입력한 다음 각각 `"body": { "model": "M1", ... }`을 구성하여 서로 다른 파라미터를 사용할 수 있습니다.
        - `body.messages`: 구성할 수 없습니다. 항상 플러그인이 프롬프트 템플릿에 따라 생성하므로 구성은 무시됩니다.
        - `body.stream`: 항상 `false`로 고정됩니다(플러그인은 스트리밍 응답을 지원하지 않음). 구성은 무시됩니다.
- `languageMapping`: 언어 매핑 테이블로, 키는 언어 코드(예: `zh-CN`) 또는 열거형 이름(예: `CHINESE_SIMPLIFIED`)이고 값은 벤더가 정의한 언어 문자열입니다. 매핑되지 않은 언어는 언어의 영어 이름으로 대체됩니다.
- `prompt`: 프롬프트 템플릿 파일 경로(상대 경로는 구성 파일이 있는 디렉토리를 기준으로 하며 절대 경로도 지원됨). 구성되지 않은 경우 기본 프롬프트 템플릿이 사용됩니다.

### 플레이스홀더

`request.headers`와 `request.body`의 문자열 값에는 플레이스홀더를 사용할 수 있습니다(이 세 가지만 대체됨):

- `${TEXT}`: 번역할 원문
- `${SOURCE_LANGUAGE}`: 원본 언어(`languageMapping`으로 매핑된 문자열)
- `${TARGET_LANGUAGE}`: 대상 언어(`languageMapping`으로 매핑된 문자열)

리터럴 `${...}` 텍스트를 출력해야 하는 경우 `$${...}`로 이스케이프할 수 있습니다. 예를 들어 `$${TEXT}`는 리터럴 `${TEXT}`로 렌더링됩니다.

프롬프트 템플릿에는 기존의 `$TEXT`, `$SOURCE_LANGUAGE`, `$TARGET_LANGUAGE` 외에도 언어 매핑 테이블로 매핑된 언어 문자열 변수 `$MAPPED_SOURCE_LANGUAGE`와 `$MAPPED_TARGET_LANGUAGE`가 새로 추가되었습니다.

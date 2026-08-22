# 在 OpenAI 翻译引擎中配置第三方 AI

我们在 [3.7](#/updates/v3.7 ':ignore :target=_blank') 版本中增强了 OpenAI 翻译引擎的可配置性，您可以通过配置 API 端点和 AI 模型，使用更多兼容 OpenAI Chat Completions API 的第三方 AI 服务。本教程将列出一些常见的第三方 AI 服务的配置方法。

> 提示：插件默认的提示词在不同的 AI 服务中的表现可能会有所不同，您可以通过自定义提示词模板来自行调整，以获得更好的翻译体验。详见：[《如何自定义 OpenAI 翻译提示词》](/tutorial/how_to_customize_translation_prompts.md)。

> 提示：自 3.8.5 版本起，支持通过请求配置文件为不同的 AI 服务或模型自定义请求参数、请求头、提示词模板与语言映射等。详见：[自定义请求配置](#custom-request-config)。

## DeepSeek :id=deepseek

- **模型**：`deepseek-v4-flash`
- **API 端点**：`https://api.deepseek.com` `/v1/chat/completions`
- **API 密钥**：[API key](https://platform.deepseek.com/api_keys)

关闭思考模式配置（插件版本 3.8.5 以上）：

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
配置说明见[自定义请求配置](#custom-request-config)。

## Doubao :id=doubao

- **模型**：`deepseek-v3-241226`
- **API 端点**：`https://ark.cn-beijing.volces.com` `/api/v3/chat/completions`
- **API 密钥**：[API key](https://www.volcengine.com/docs/82379/1399008#b00dee71)

?> 其他模型请参阅：[模型列表](https://www.volcengine.com/docs/82379/1330310)

## Gemini :id=gemini

- **模型**：`gemini-2.0-flash`
- **API 端点**：`https://generativelanguage.googleapis.com` `/v1beta/openai/chat/completions`
- **API 密钥**：[API key](https://ai.google.dev/gemini-api/docs/api-key?hl=zh-cn)

?> 其他模型请参阅：[Gemini 模型](https://ai.google.dev/gemini-api/docs/models/gemini?hl=zh-cn)

## Kimi :id=kimi

- **模型**：`moonshot-v1-8k`
- **API 端点**：`https://api.moonshot.cn` `/v1/chat/completions`
- **API 密钥**：[API key](https://platform.moonshot.cn/console/api-keys)

?> 其他模型请参阅：[模型与定价](https://platform.moonshot.cn/docs/pricing/chat)（注意，目前不支持使用 `kimi-latest` 模型）

## Ollama :id=ollama

- **模型**：*\<正在运行的模型，例如：`llama3.2:1b`>*
- **API 端点**：`http://localhost:11434` `/v1/chat/completions`
- **API 密钥**：*\<必须但忽略>*

?> 提示：API 密钥配置项是必须的，但 Ollama 会忽略它，因此可以填写任意非空值。

## Qwen :id=qwen

- **模型**：`qwen-max`
- **API 端点**：`https://dashscope.aliyuncs.com` `/compatible-mode/v1/chat/completions`
- **API 密钥**：[API key](https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key#ca06817d4cqro)

?> 其他模型请参阅：[模型列表](https://help.aliyun.com/zh/model-studio/developer-reference/compatibility-of-openai-with-dashscope#7f9c78ae99pwz)

### Qwen-MT :id=qwen-mt

想要使用基于 Qwen3 模型优化的机器翻译大语言模型 Qwen-MT，以 `qwen-mt-flash` 为例，可按以下步骤配置：

- 升级插件到 3.8.5 版本以上
- 在**OpenAI 翻译设置对话框**配置 Qwen-MT 模型为：`qwen-mt-flash`
- 在配置目录（可在 **OpenAI 翻译设置对话框** ⇒ <kbd>编辑请求配置...</kbd> ⇒ <kbd>打开配置目录</kbd> 快速打开）下创建文件 `prompts/qwen-mt.prompt`，文件内容为：
```
$TEXT
```
- 编辑请求配置文件（**OpenAI 翻译设置对话框** ⇒ <kbd>编辑请求配置...</kbd>）：
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

配置说明见[自定义请求配置](#custom-request-config)。

## 自定义请求配置 :id=custom-request-config

不同厂商的 API 虽然兼容 OpenAI Chat Completions API，但它们可能会有自定义的参数（位于请求体中或请求头上），且不同的模型可能有不同的参数要求。自 3.8.5 版本起，您可以通过请求配置文件为不同的 AI 服务或模型自定义请求参数、请求头、提示词模板和语言映射等。

!> 请求配置仅对翻译（Chat Completions）生效，TTS 和服务商选 Azure 时不支持。

!> 这是一项实验性的功能，在未来的版本中随时有可能被放弃或更改。

### 配置文件

配置文件位于插件的 `openai/config.json` 文件中：

- Windows：`%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\openai\config.json`
- 其他系统：`$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/openai/config.json` 或 `~/.TranslationPlugin/openai/config.json`

您可以在 OpenAI 翻译设置对话框中点击左下角的 **编辑请求配置...** 按钮来编辑该配置文件：在对话框中直接编辑时，IDE 会根据随附的 JSON Schema 提供校验与补全；您也可以通过 **在主编辑器中打开** 按钮在 IDE 主编辑器中进行编辑（推荐使用，体验更佳，可以查看配置字段文档），或通过 **打开配置目录** 按钮在系统资源管理器中打开配置目录。

### 配置示例

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

Schema:  [openai-config.schema.json](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/refs/tags/v3.8.5/src/main/resources/schemas/openai-config.schema.json)

您的编辑器应该能够基于该 Schema 进行验证和自动补全。

### 配置说明

- `default`：默认配置，所有模型共用。
- `models`：按模型配置，键为 UI 中选择的模型 ID（或自定义模型名）。模型配置会与 `default` 进行深合并，相同的配置项会被覆盖。
- `request`：请求定制。
    - `request.headers`：额外的 HTTP 请求头。
    - `request.body`：额外的请求体字段（任意 JSON 值）。
        - `body.model`：可用于覆盖请求的模型 ID。例如同一个开源模型 `M1` 被 A、B 两个厂商以相同模型 ID 部署时，可在 UI 中分别填写 `A/M1` 和 `B/M1`，然后分别配置 `"body": { "model": "M1", ... }` 以使用不同的参数。
        - `body.messages`：不可配置，始终由插件根据提示词模板生成，配置无效。
        - `body.stream`：固定为 `false`（插件不支持流式响应），配置无效。
- `languageMapping`：语言映射表，键为语言代码（如 `zh-CN`）或枚举名（如 `CHINESE_SIMPLIFIED`），值为厂商定义的语言字符串。未映射的语言会回退为语言英文名。
- `prompt`：提示词模板文件路径（相对路径相对于配置文件所在目录，也支持绝对路径）。未配置时使用默认的提示词模板。

### 占位符

`request.headers` 和 `request.body` 的字符串值中可使用占位符（仅这三个会被替换）：

- `${TEXT}`：要翻译的原文
- `${SOURCE_LANGUAGE}`：原语言（经 `languageMapping` 映射后的字符串）
- `${TARGET_LANGUAGE}`：目标语言（经 `languageMapping` 映射后的字符串）

如果需要输出字面量的 `${...}` 文本，可使用 `$${...}` 转义，例如 `$${TEXT}` 会被渲染为字面量 `${TEXT}`。

提示词模板中除了原有的 `$TEXT`、`$SOURCE_LANGUAGE`、`$TARGET_LANGUAGE` 外，还新增了经语言映射表映射后的语言字符串变量 `$MAPPED_SOURCE_LANGUAGE` 和 `$MAPPED_TARGET_LANGUAGE`。

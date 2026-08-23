# Configuring Third-Party AI in OpenAI Translator

In version [3.7](#/en/updates/v3.7 ':ignore :target=_blank'), we enhanced the configurability of the OpenAI translation engine. You can now configure the API endpoint and AI model to use more third-party AI services compatible with the OpenAI Chat Completions API. This tutorial will list the configuration methods for some common third-party AI services.

> Tip: The default prompts in the plugin may perform differently across various AI services. You can adjust them by customizing the prompt template to achieve a better translation experience. For more details, see: [How to Customize OpenAI Translation Prompts](/en/tutorial/how_to_customize_translation_prompts.md).

> Tip: Since version [3.9](#/en/updates/v3.9 ':ignore :target=_blank'), you can customize request parameters, request headers, prompt templates, and language mappings for different AI services or models via request configuration files. For more details, see: [Custom Request Configuration](#custom-request-config).

## DeepSeek :id=deepseek

- **Model**: `deepseek-v4-flash`
- **API Endpoint**: `https://api.deepseek.com` `/v1/chat/completions`
- **API Key**: [API key](https://platform.deepseek.com/api_keys)

To disable thinking mode (plugin version [3.9.0](https://plugins.jetbrains.com/plugin/8579-translation/versions) or later):

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
For configuration details, see [Custom Request Configuration](#custom-request-config).

## Doubao :id=doubao

- **Model**: `deepseek-v3-241226`
- **API Endpoint**: `https://ark.cn-beijing.volces.com` `/api/v3/chat/completions`
- **API Key**: [API key](https://www.volcengine.com/docs/82379/1399008#b00dee71)

?> For other models, refer to: [Model List](https://www.volcengine.com/docs/82379/1330310)

## Gemini :id=gemini

- **Model**: `gemini-2.0-flash`
- **API Endpoint**: `https://generativelanguage.googleapis.com` `/v1beta/openai/chat/completions`
- **API Key**: [API key](https://ai.google.dev/gemini-api/docs/api-key)

?> For other models, refer to: [Gemini Models](https://ai.google.dev/gemini-api/docs/models/gemini)

## Kimi :id=kimi

- **Model**: `moonshot-v1-8k`
- **API Endpoint**: `https://api.moonshot.cn` `/v1/chat/completions`
- **API Key**: [API key](https://platform.moonshot.cn/console/api-keys)

?> For other models, refer to: [Models and Pricing](https://platform.moonshot.cn/docs/pricing/chat) (Note: The `kimi-latest` model is currently not supported.)

## Ollama :id=ollama

- **Model**: *\<The model you are running, e.g., `llama3.2:1b`>*
- **API Endpoint**: `http://localhost:11434` `/v1/chat/completions`
- **API Key**: *\<Required but ignored>*

?> Tip: The API key field is required, but Ollama ignores it, so you can fill in any non-empty value.

## Qwen :id=qwen

- **Model**: `qwen-max`
- **API Endpoint**: `https://dashscope.aliyuncs.com` `/compatible-mode/v1/chat/completions`
- **API Key**: [API key](https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key#ca06817d4cqro)

?> For other models, refer to: [Model List](https://help.aliyun.com/zh/model-studio/developer-reference/compatibility-of-openai-with-dashscope#7f9c78ae99pwz)

### Qwen-MT :id=qwen-mt

To use Qwen-MT, an LLM optimized for machine translation based on the Qwen3 model, take `qwen-mt-flash` as an example and follow the steps below:

- Upgrade the plugin to version [3.9.0](https://plugins.jetbrains.com/plugin/8579-translation/versions) or later.
- In the **OpenAI Translator Settings dialog**, configure a custom model as the Qwen-MT model: `qwen-mt-flash`.
- In the configuration directory (which can be opened quickly via the **OpenAI Translator Settings dialog** ⇒ <kbd>Edit Request Config...</kbd> ⇒ <kbd>Open Config Directory</kbd>), create a file `prompts/qwen-mt.prompt` with the following content:
```
$TEXT
```
- Edit the request configuration file (**OpenAI Translator Settings dialog** ⇒ <kbd>Edit Request Config...</kbd>):
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

For configuration details, see [Custom Request Configuration](#custom-request-config).

## Custom Request Configuration :id=custom-request-config

Although APIs from different vendors are compatible with the OpenAI Chat Completions API, they may have custom parameters (in the request body or request headers), and different models may have different parameter requirements. Since version [3.9](#/en/updates/v3.9 ':ignore :target=_blank'), you can customize request parameters, request headers, prompt templates, and language mappings for different AI services or models via request configuration files.

!> Request configuration only applies to translation (Chat Completions). It is not supported for TTS or when Azure is selected as the provider.

!> This is an experimental feature and may be abandoned or changed at any time in future versions.

### Configuration File

The configuration file is the plugin's `openai/config.json` file:

- Windows: `%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\openai\config.json`
- Other systems: `$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/openai/config.json` or `~/.TranslationPlugin/openai/config.json`

You can edit this configuration file by clicking the **Edit Request Config...** button in the bottom-left corner of the OpenAI Translator Settings dialog: when editing directly in the dialog, the IDE provides validation and completion based on the bundled JSON Schema; you can also edit it in the IDE main editor via the **Open in Main Editor** button (recommended, for a better experience and to view the documentation of the configuration fields), or open the configuration directory in the system file manager via the **Open Config Directory** button.

### Configuration Example

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

Schema: [openai-config.schema.json](https://raw.githubusercontent.com/YiiGuxing/TranslationPlugin/refs/tags/v3.9.0/src/main/resources/schemas/openai-config.schema.json)

Your editor should be able to validate and autocomplete based on this Schema.

### Configuration Details

- `default`: Default configuration shared by all models.
- `models`: Per-model configuration, keyed by the model ID (or custom model name) selected in the UI. Model configurations are deep-merged with `default`, and identical configuration items are overridden.
- `request`: Request customization.
    - `request.headers`: Additional HTTP request headers.
    - `request.body`: Additional request body fields (arbitrary JSON values).
        - `body.model`: Can be used to override the model ID in the request. For example, when the same open-source model `M1` is deployed by vendors A and B with the same model ID, you can enter `A/M1` and `B/M1` in the UI respectively, and then configure `"body": { "model": "M1", ... }` for each to use different parameters.
        - `body.messages`: Not configurable. It is always generated by the plugin from the prompt template, so the configuration is ignored.
        - `body.stream`: Fixed to `false` (the plugin does not support streaming responses), so the configuration is ignored.
- `languageMapping`: Language mapping table. Keys are language codes (e.g., `zh-CN`) or enum names (e.g., `CHINESE_SIMPLIFIED`), and values are language strings defined by the vendor. Unmapped languages fall back to the English name of the language.
- `prompt`: Path to the prompt template file (relative paths are relative to the directory containing the configuration file; absolute paths are also supported). If not configured, the default prompt templates are used.

### Placeholders

Placeholders can be used in string values of `request.headers` and `request.body` (only these three are replaced):

- `${TEXT}`: The original text to be translated
- `${SOURCE_LANGUAGE}`: The source language (the string after `languageMapping` mapping)
- `${TARGET_LANGUAGE}`: The target language (the string after `languageMapping` mapping)

If you need to output a literal `${...}` text, you can escape it with `$${...}`. For example, `$${TEXT}` will be rendered as the literal `${TEXT}`.

In addition to the original `$TEXT`, `$SOURCE_LANGUAGE`, and `$TARGET_LANGUAGE` in prompt templates, two new variables are added: `$MAPPED_SOURCE_LANGUAGE` and `$MAPPED_TARGET_LANGUAGE`, which are the language strings mapped through the language mapping table.

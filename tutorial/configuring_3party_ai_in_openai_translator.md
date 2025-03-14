# 在 OpenAI 翻译引擎中配置第三方 AI

我们在 [3.7](#/updates/v3.7 ':ignore :target=_blank') 版本中增强了 OpenAI 翻译引擎的可配置性，您可以通过配置 API 端点和 AI 模型，使用更多兼容 OpenAI Chat API 的第三方 AI 服务。本教程将列出一些常见的第三方 AI 服务的配置方法。

> 提示：插件默认的提示词在不同的 AI 服务中的表现可能会有所不同，您可以通过自定义提示词模板来自行调整，以获得更好的翻译体验。详见：[《如何自定义 OpenAI 翻译提示词》](/tutorial/how_to_customize_translation_prompts.md)。

## DeepSeek :id=deepseek

- **模型**：`deepseek-chat`
- **API 端点**：`https://api.deepseek.com` `/v1/chat/completions`
- **API 密钥**：[API key](https://platform.deepseek.com/api_keys)

?> 提示：不建议使用 `deepseek-reasoner` 模型，这是一个专门用于推理的模型，它会使用更长的时间进行推理思考，在输出最终回答之前，模型还会先输出一段思维链内容，且思维链内容在此接口中无法关闭。

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

- **模型**：*\<你正在运行的模型，例如：`llama3.2:1b`>*
- **API 端点**：`http://localhost:11434` `/v1/chat/completions`
- **API 密钥**：*\<必须但忽略>*

?> 提示：API 密钥配置项是必须的，但 Ollama 会忽略它，因此可以填写任意非空值。

## Qwen :id=qwen

- **模型**：`qwen-max`
- **API 端点**：`https://dashscope.aliyuncs.com` `/compatible-mode/v1/chat/completions`
- **API 密钥**：[API key](https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key#ca06817d4cqro)

?> 其他模型请参阅：[模型列表](https://help.aliyun.com/zh/model-studio/developer-reference/compatibility-of-openai-with-dashscope#7f9c78ae99pwz)

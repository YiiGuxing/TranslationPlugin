# Configuring Third-Party AI in OpenAI Translator

In version [3.7](#/updates/v3.7 ':ignore :target=_blank'), we enhanced the configurability of the OpenAI translation engine. You can now configure the API endpoint and AI model to use more third-party AI services compatible with the OpenAI Chat API. This tutorial will list the configuration methods for some common third-party AI services.

> Tip: The default prompts in the plugin may perform differently across various AI services. You can adjust them by customizing the prompt template to achieve a better translation experience. For more details, see: [How to Customize OpenAI Translation Prompts](/en/tutorial/how_to_customize_translation_prompts.md).

## DeepSeek :id=deepseek

- **Model**: `deepseek-chat`
- **API Endpoint**: `https://api.deepseek.com` `/v1/chat/completions`
- **API Key**: [API key](https://platform.deepseek.com/api_keys)

?> Tip: It is not recommended to use the `deepseek-reasoner` model, as it is specifically designed for reasoning tasks. It takes longer to process and outputs a chain of thought before providing the final answer. Additionally, the chain of thought cannot be disabled in this interface.

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
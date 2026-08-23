# OpenAI 翻訳エンジンでのサードパーティ AI の設定

[3.7](#/updates/v3.7 ':ignore :target=_blank') バージョンでは、OpenAI 翻訳エンジンの設定可能性を強化しました。API エンドポイントと AI モデルを設定することで、OpenAI Chat Completions API と互換性のある、より多くのサードパーティ AI サービスを利用できます。本チュートリアルでは、一般的なサードパーティ AI サービスの設定方法をいくつか紹介します。

> ヒント：プラグインのデフォルトのプロンプトは、AI サービスによって動作が異なる場合があります。プロンプトテンプレートをカスタマイズして調整することで、より良い翻訳体験を得られます。詳細は [OpenAI 翻訳プロンプトのカスタマイズ方法](/ja/tutorial/how_to_customize_translation_prompts.md) をご参照ください。

> ヒント：[3.9](#/ja/updates/v3.9 ':ignore :target=_blank') バージョン以降、リクエスト設定ファイルを使用して、AI サービスやモデルごとにリクエストパラメータ、リクエストヘッダー、プロンプトテンプレート、言語マッピングなどをカスタマイズできます。詳細は [カスタムリクエスト設定](#custom-request-config) をご参照ください。

## DeepSeek :id=deepseek

- **モデル**：`deepseek-v4-flash`
- **API エンドポイント**：`https://api.deepseek.com` `/v1/chat/completions`
- **API キー**：[API key](https://platform.deepseek.com/api_keys)

思考モードを無効にする設定（プラグイン [3.9.0](https://plugins.jetbrains.com/plugin/8579-translation/versions) 以降）：

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
設定の詳細は [カスタムリクエスト設定](#custom-request-config) をご参照ください。

## Doubao :id=doubao

- **モデル**：`deepseek-v3-241226`
- **API エンドポイント**：`https://ark.cn-beijing.volces.com` `/api/v3/chat/completions`
- **API キー**：[API key](https://www.volcengine.com/docs/82379/1399008#b00dee71)

?> その他のモデルについては [モデルリスト](https://www.volcengine.com/docs/82379/1330310) をご参照ください。

## Gemini :id=gemini

- **モデル**：`gemini-2.0-flash`
- **API エンドポイント**：`https://generativelanguage.googleapis.com` `/v1beta/openai/chat/completions`
- **API キー**：[API key](https://ai.google.dev/gemini-api/docs/api-key?hl=ja)

?> その他のモデルについては [Gemini モデル](https://ai.google.dev/gemini-api/docs/models/gemini?hl=ja) をご参照ください。

## Kimi :id=kimi

- **モデル**：`moonshot-v1-8k`
- **API エンドポイント**：`https://api.moonshot.cn` `/v1/chat/completions`
- **API キー**：[API key](https://platform.moonshot.cn/console/api-keys)

?> その他のモデルについては [モデルと料金](https://platform.moonshot.cn/docs/pricing/chat) をご参照ください（注：現在 `kimi-latest` モデルはサポートされていません）。

## Ollama :id=ollama

- **モデル**：*\<実行中のモデル（例：`llama3.2:1b`）>*
- **API エンドポイント**：`http://localhost:11434` `/v1/chat/completions`
- **API キー**：*\<必須ですが無視されます>*

?> ヒント：API キーの設定項目は必須ですが、Ollama はそれを無視するため、任意の空でない値を入力できます。

## Qwen :id=qwen

- **モデル**：`qwen-max`
- **API エンドポイント**：`https://dashscope.aliyuncs.com` `/compatible-mode/v1/chat/completions`
- **API キー**：[API key](https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key#ca06817d4cqro)

?> その他のモデルについては [モデルリスト](https://help.aliyun.com/zh/model-studio/developer-reference/compatibility-of-openai-with-dashscope#7f9c78ae99pwz) をご参照ください。

### Qwen-MT :id=qwen-mt

Qwen3 モデルを基に最適化された機械翻訳 LLM「Qwen-MT」を使用する場合、`qwen-mt-flash` を例に、以下の手順で設定します：

- プラグインを [3.9.0](https://plugins.jetbrains.com/plugin/8579-translation/versions) 以降にアップグレードします
- **OpenAI 翻訳設定ダイアログ**でカスタムモデルを Qwen-MT モデルとして設定します：`qwen-mt-flash`
- 設定ディレクトリ（**OpenAI 翻訳設定ダイアログ** ⇒ <kbd>リクエスト設定を編集...</kbd> ⇒ <kbd>エディターで開く</kbd> で素早く開けます）に `prompts/qwen-mt.prompt` ファイルを作成し、内容を次のようにします：
```
$TEXT
```
- リクエスト設定ファイルを編集します（**OpenAI 翻訳設定ダイアログ** ⇒ <kbd>リクエスト設定を編集...</kbd>）：
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

設定の詳細は [カスタムリクエスト設定](#custom-request-config) をご参照ください。

## カスタムリクエスト設定 :id=custom-request-config

各ベンダーの API は OpenAI Chat Completions API と互換性がありますが、カスタムパラメータ（リクエストボディまたはリクエストヘッダー内）を持つ場合があり、モデルによって必要なパラメータが異なる場合があります。[3.9](#/en/updates/v3.9 ':ignore :target=_blank') バージョン以降、リクエスト設定ファイルを使用して、AI サービスやモデルごとにリクエストパラメータ、リクエストヘッダー、プロンプトテンプレート、言語マッピングなどをカスタマイズできます。

!> リクエスト設定は翻訳（Chat Completions）にのみ有効です。TTS とプロバイダーが Azure の場合はサポートされません。

!> これは実験的な機能であり、将来のバージョンでいつでも廃止または変更される可能性があります。

### 設定ファイル

設定ファイルはプラグインの `openai/config.json` ファイルにあります：

- Windows：`%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\openai\config.json`
- その他のシステム：`$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/openai/config.json` または `~/.TranslationPlugin/openai/config.json`

OpenAI 翻訳設定ダイアログの左下にある **リクエスト設定を編集...** ボタンをクリックして、この設定ファイルを編集できます。ダイアログ内で直接編集する場合、IDE は付属の JSON Schema に基づいて検証と補完を提供します。**メインエディタで開く** ボタンで IDE のメインエディタで編集することもできます（推奨。より良い体験が得られ、設定フィールドのドキュメントを確認できます）。または、**エディターで開く** ボタンでシステムのファイルマネージャーでエディターで開くこともできます。

### 設定例

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

お使いのエディタは、この Schema に基づいて検証と自動補完ができるはずです。

### 設定の説明

- `default`：デフォルト設定。すべてのモデルで共有されます。
- `models`：モデルごとの設定。キーは UI で選択されたモデル ID（またはカスタムモデル名）です。モデル設定は `default` と深いマージ（deep merge）され、同じ設定項目は上書きされます。
- `request`：リクエストのカスタマイズ。
    - `request.headers`：追加の HTTP リクエストヘッダー。
    - `request.body`：追加のリクエストボディフィールド（任意の JSON 値）。
        - `body.model`：リクエストのモデル ID を上書きするために使用できます。例えば、同じオープンソースモデル `M1` が A、B の 2 つのベンダーによって同じモデル ID でデプロイされている場合、UI でそれぞれ `A/M1` と `B/M1` を入力し、それぞれに `"body": { "model": "M1", ... }` を設定することで、異なるパラメータを使用できます。
        - `body.messages`：設定できません。常にプラグインがプロンプトテンプレートに基づいて生成するため、設定は無効です。
        - `body.stream`：常に `false` に固定されます（プラグインはストリーミングレスポンスをサポートしていません）。設定は無効です。
- `languageMapping`：言語マッピングテーブル。キーは言語コード（例：`zh-CN`）または列挙名（例：`CHINESE_SIMPLIFIED`）、値はベンダーが定義する言語文字列です。マッピングされていない言語は英語名にフォールバックします。
- `prompt`：プロンプトテンプレートファイルのパス（相対パスは設定ファイルのあるディレクトリを基準とし、絶対パスもサポートされます）。未設定の場合はデフォルトのプロンプトテンプレートが使用されます。

### プレースホルダー

`request.headers` と `request.body` の文字列値ではプレースホルダーを使用できます（この 3 つのみが置換されます）：

- `${TEXT}`：翻訳する原文
- `${SOURCE_LANGUAGE}`：元の言語（`languageMapping` でマッピングされた文字列）
- `${TARGET_LANGUAGE}`：ターゲット言語（`languageMapping` でマッピングされた文字列）

リテラルの `${...}` テキストを出力する必要がある場合は、`$${...}` でエスケープできます。例えば、`$${TEXT}` はリテラルの `${TEXT}` としてレンダリングされます。

プロンプトテンプレートには、従来の `$TEXT`、`$SOURCE_LANGUAGE`、`$TARGET_LANGUAGE` に加えて、言語マッピングテーブルでマッピングされた言語文字列変数 `$MAPPED_SOURCE_LANGUAGE` と `$MAPPED_TARGET_LANGUAGE` が新たに追加されました。

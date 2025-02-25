# 如何自定义 OpenAI 翻译提示词

我们在 [3.7](#/updates/v3.7 ':ignore :target=_blank') 版本中为 OpenAI 翻译引擎新增了自定义翻译提示词模板的功能，您可以通过提示词模板定制您自己的翻译提示词，以便更好地适应您的翻译需求。在本教程中，您将学习如何自定义翻译提示词。

## 配置翻译提示词模板文件 :id=template-file

目前，我们仅支持通过配置文件的方式来自定义翻译提示词模板。模板文件有常规翻译提示词模板和文档翻译提示词模板两个文件：

- 常规翻译：`translator.prompt`
- 文档翻译：`document.prompt`

模板文件目录路径：

- Windows：`%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\openai\`
- 其他：`$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/openai/` 或 `~/.TranslationPlugin/openai/`

?> 提示：提示词模板文件和目录默认情况下不会自动生成，您可能需要手动创建这些文件和目录。

如果您想要自定义翻译提示词，您可以在模板目录下创建对应的提示词模板文件，然后在文件中编写您的翻译提示词模板即可。否则，OpenAI 翻译引擎将使用默认的翻译提示词模板。

## 编写翻译提示词模板 :id=write-template

翻译提示词模板内容格式如下：

```plaintext
[ROLE1]
Message 1
[ROLE2]
Message 2
...
```

一个模板文件中至少需要包含一条**消息模板**，一条消息模板又包含**消息角色**和**消息体**两个部分。

### 消息角色 :id=message-role

消息角色用于指定消息的角色，也是消息模板的标识，用于区分不同的消息模板。消息角色的格式为 `[角色名]`，其中 `角色名` 为消息角色的名称，目前支持 `SYSTEM`，`USER` 和 `ASSISTANT` 三个角色。模板文件中的第一条消息可以省略消息角色的定义，如果第一条消息未指定角色，则该消息默认为 `USER` 角色。

!> **注意：** 消息角色的定义必须独占一行，由 `[` 开始，`]` 结束，并且 `[` 前和 `]` 后不能有其他字符，包括空白字符。

### 消息体 :id=message-body

任何在两个消息角色定义之间的内容或者角色定义到文件结尾（中间不存在消息角色定义）的内容都被视为消息体，包括空行。消息体有效内容的前后空行或空白行将被忽略（有效内容内部的空行或空白行会被保留）。
另外，消息体所有行的公共缩进也会被忽略，因此如果你的消息体里有和消息角色定义一样的内容时，为了避免其被解析为消息角色定义，你可以为消息体的每一行添加相同的缩进：

```plaintext
[USER]
  Message line 1
  [SYSTEM]
  Message line 3
  ...
```

最终它会被渲染为：

- 消息角色：USER
- 消息体：
  ```plaintext
  Message line 1
  [SYSTEM]
  Message line 3
  ...
  ```

### 模板引擎 :id=template-engine

翻译提示词模板使用 [Apache Velocity](https://velocity.apache.org/engine/devel/user-guide.html) 模板引擎进行解析与渲染。因此你可以在模板中使用 Apache Velocity 模板语言进行编写。

#### 模板变量 :id=template-variables

| **变量** | **说明** |
|:-------:|-----------|
| ${DS} | 符号 `$`，此变量用于转义 `$` 字符 |
| ${UTILS} | 工具对象，集成了一些有用的工具函数，见 [Utils](https://github.com/YiiGuxing/TranslationPlugin/blob/v3.7.0/src/main/kotlin/cn/yiiguxing/plugin/translate/trans/openai/prompt/template/Utils.kt) |
| ${LANGUAGE} | 语言枚举类，提供语言枚举和一些工具函数，可通过 `${LANGUAGE.语言名}` 获取指定语言枚举对象（如：`${LANGUAGE.CHINESE}`），见 [LanguageEnum](https://github.com/YiiGuxing/TranslationPlugin/blob/v3.7.0/src/main/kotlin/cn/yiiguxing/plugin/translate/trans/openai/prompt/template/LanguageEnum.kt) 和 [Lang](https://github.com/YiiGuxing/TranslationPlugin/blob/v3.7.0/src/main/kotlin/cn/yiiguxing/plugin/translate/trans/Languages.kt#L28) |
| ${TEXT} | 要翻译的文本，如果是文档翻译，将是 HTML 格式的文本 |
| ${SOURCE_LANGUAGE} | 翻译文本的原语言枚举对象，见 [Lang](https://github.com/YiiGuxing/TranslationPlugin/blob/v3.7.0/src/main/kotlin/cn/yiiguxing/plugin/translate/trans/Languages.kt#L28)（注：可能为 `AUTO`，需要特别处理） |
| ${TARGET_LANGUAGE} | 翻译目标语言枚举对象，见 [Lang](https://github.com/YiiGuxing/TranslationPlugin/blob/v3.7.0/src/main/kotlin/cn/yiiguxing/plugin/translate/trans/Languages.kt#L28) |

### 默认提示词模板 :id=default-templates

translator.prompt

```plaintext
[SYSTEM]
You are a translator.
The user will provide you with text in triple quotes.
Translate the text#if($LANGUAGE.isExplicit($SOURCE_LANGUAGE)) from ${SOURCE_LANGUAGE.languageName} to#else into#end ${TARGET_LANGUAGE.languageName}.
Do not return the translated text in triple quotes.

[USER]
"""
$TEXT
"""
```
document.prompt

```plaintext
[SYSTEM]
You are a html document translator.
The user will provide you with an html document.
Translate the html document#if($LANGUAGE.isExplicit($SOURCE_LANGUAGE)) from ${SOURCE_LANGUAGE.languageName} to#else into#end ${TARGET_LANGUAGE.languageName}.
Do not translate the content inside "pre" and "code" tags.

[USER]
$TEXT
```
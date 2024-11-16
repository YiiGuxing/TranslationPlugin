package cn.yiiguxing.plugin.translate.trans

import com.intellij.openapi.diagnostic.Attachment

object TranslationAttachmentFactory {

    fun createRequestAttachment(
        translator: Translator,
        requestText: String,
        srcLang: Lang,
        targetLang: Lang
    ): Attachment {
        return Attachment(
            "request.txt",
            "Translator: ${translator.id}\n" +
                    "Source language: ${srcLang.localeName}(${srcLang.code})\n" +
                    "Target language: ${targetLang.localeName}(${targetLang.code})\n" +
                    "================ Request Text ================\n" +
                    requestText +
                    "\n=============================================="
        ).apply { isIncluded = true }
    }

    fun createTranslationAttachment(translation: String): Attachment {
        return Attachment("translation.txt", translation).apply { isIncluded = true }
    }

}
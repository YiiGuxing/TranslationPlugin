package cn.yiiguxing.plugin.translate.documentation

import cn.yiiguxing.plugin.translate.Settings
import cn.yiiguxing.plugin.translate.service.TranslationCoroutineService
import cn.yiiguxing.plugin.translate.ui.scaled
import cn.yiiguxing.plugin.translate.util.toImage
import cn.yiiguxing.plugin.translate.util.toRGBHex
import com.intellij.lang.Language
import com.intellij.model.Pointer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.*
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.ui.JBColor
import icons.TranslationIcons
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import org.jsoup.nodes.Document
import java.awt.Image


/**
 * A [DocumentationTarget] that supports translation.
 */
@Suppress("UnstableApiUsage")
internal class TranslatableDocumentationTarget private constructor(
    val delegate: DocumentationTarget,
    private val pointer: TranslatableDocumentationTargetPointer
) : DocumentationTarget by delegate {

    /**
     * Indicates whether the documentation target should be translated.
     */
    var translate: Boolean
        get() = pointer.translate
        set(value) {
            pointer.translate = value
        }

    init {
        check(delegate !is TranslatableDocumentationTarget) {
            "TranslatableDocumentationTarget should not be nested: $delegate"
        }
    }

    constructor(
        project: Project,
        language: Language,
        delegate: DocumentationTarget,
        translate: Boolean = service<Settings>().translateDocumentation
    ) : this(
        delegate = delegate,
        pointer = DefaultTranslatableDocumentationTargetPointer(
            project = project,
            language = language,
            delegatePointer = createPointer(delegate),
            translate = translate
        )
    )

    constructor(
        project: Project,
        language: Language,
        delegate: DocumentationTarget,
        psiElement: PsiElement
    ) : this(
        delegate = delegate,
        pointer = TranslatablePsiDocumentationTargetPointer(
            project = project,
            language = language,
            delegatePointer = createPointer(delegate),
            psiElementPointer = psiElement.createSmartPointer()
        )
    )

    override fun createPointer(): Pointer<out DocumentationTarget> = pointer

    override fun computeDocumentation(): DocumentationResult? {
        val originalResult = delegate.computeDocumentation()
        if (!translate ||
            originalResult == null ||
            !(originalResult is AsyncDocumentation || originalResult is DocumentationData)
        ) {
            return originalResult
        }

        return DocumentationResult.asyncDocumentation {
            val documentation = when (originalResult) {
                is AsyncDocumentation -> originalResult.supplier()
                is DocumentationResult.Documentation -> originalResult
            }
            if (documentation !is DocumentationData) {
                return@asyncDocumentation documentation
            }

            val contentUpdates = MutableSharedFlow<DocumentationContent>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            val translationJob = TranslationCoroutineService.projectScope(pointer.project).launch {
                // TODO: 1. Reuse the original image map of `DocumentationData.content`.
                //       2. Translate the updates from `DocumentationData.updates`.
                val document: Document = Documentations.parseDocumentation(documentation.html)
                document.setMessage("Translating...")
                contentUpdates.tryEmit(DocumentationContent.content(document.documentationString, getIconMap()))

                // TODO: 1. Implement translation logic here.
                //       2. Return the translated version of the documentation.
                // doTranslate(document.clone(), pointer.language)

                delay(3000)
                document.setMessage("Translation failed. Please try again later.", true)
                contentUpdates.tryEmit(DocumentationContent.content(document.documentationString, getIconMap()))
            }

            documentation.updates(contentUpdates.onCompletion { translationJob.cancel() })
        }
    }


    private sealed interface TranslatableDocumentationTargetPointer : Pointer<TranslatableDocumentationTarget> {
        val project: Project
        val language: Language
        var translate: Boolean
    }

    private class DefaultTranslatableDocumentationTargetPointer(
        override val project: Project,
        override val language: Language,
        private val delegatePointer: Pointer<out DocumentationTarget>,
        @Volatile override var translate: Boolean
    ) : TranslatableDocumentationTargetPointer {
        override fun dereference(): TranslatableDocumentationTarget? {
            val target = delegatePointer.dereference() ?: return null
            return TranslatableDocumentationTarget(target, this)
        }
    }

    private class TranslatablePsiDocumentationTargetPointer(
        override val project: Project,
        override val language: Language,
        private val delegatePointer: Pointer<out DocumentationTarget>,
        private val psiElementPointer: Pointer<out PsiElement>
    ) : TranslatableDocumentationTargetPointer {

        private val settings: Settings by lazy { service<Settings>() }

        override var translate: Boolean
            get() = psiElementPointer.dereference()
                ?.let { DocTranslationService.getTranslationState(it) ?: settings.translateDocumentation }
                ?: false
            set(value) {
                psiElementPointer.dereference()?.let {
                    DocTranslationService.setTranslationState(it, value)
                }
            }

        override fun dereference(): TranslatableDocumentationTarget? {
            val target = delegatePointer.dereference() ?: return null
            psiElementPointer.dereference() ?: return null

            return TranslatableDocumentationTarget(target, this)
        }
    }

    companion object {
        private const val ICON_URL_TRANSLATION = "http://img/TranslationIcons.Translation"
        private const val ICON_URL_TRANSLATION_FAILED = "http://img/TranslationIcons.TranslationFailed"

        private const val MESSAGE_WRAPPER_ID = "translation-msg-wrapper"
        private const val ICON_ELEMENT_ID = "translation-icon"
        private const val MESSAGE_ELEMENT_ID = "translation-msg"

        private val ICON_MAPS: Array<Map<String, Image>?> = Array(2) { null }


        @Suppress("OverrideOnly", "UnstableApiUsage")
        private fun createPointer(target: DocumentationTarget): Pointer<out DocumentationTarget> {
            ApplicationManager.getApplication().assertReadAccessAllowed()
            return target.createPointer()
        }

        private fun getIconMap(): Map<String, Image> {
            return synchronized(ICON_MAPS) {
                val index = if (JBColor.isBright()) 0 else 1
                ICON_MAPS[index] ?: createIconMap().also { ICON_MAPS[index] = it }
            }
        }

        private fun createIconMap(): Map<String, Image> {
            return HashMap<String, Image>().apply {
                TranslationIcons.Translation.toImage()?.let { put(ICON_URL_TRANSLATION, it) }
                TranslationIcons.TranslationFailed.toImage()?.let { put(ICON_URL_TRANSLATION_FAILED, it) }
            }
        }

        private fun Document.setMessage(message: String, isError: Boolean = false): Document = apply {
            val color = JBColor(0x5E5E5E, 0xAFB1B3).toRGBHex()
            val contentEl = body().selectFirst(CSS_QUERY_CONTENT) ?: return@apply
            val messageWrapperEl = contentEl.selectFirst("#$MESSAGE_WRAPPER_ID")
            if (messageWrapperEl == null) {
                val trEl = contentEl.prependElement("div")
                    .id(MESSAGE_WRAPPER_ID)
                    .attr(
                        "style",
                        "color: $color;" +
                                "padding-left: ${3.scaled}px;" +
                                "margin: ${5.scaled}px 0;" +
                                "border-left: 2px rgba(128,128,128,0.3) solid;"
                    )
                    .appendElement("table")
                    .appendElement("tbody")
                    .appendElement("tr")

                val iconUrl = if (isError) ICON_URL_TRANSLATION_FAILED else ICON_URL_TRANSLATION
                trEl.appendElement("td")
                    .attr("style", "margin: 0 ${2.scaled}px 0 0; padding: 0;")
                    .appendElement("img")
                    .id(ICON_ELEMENT_ID)
                    .attr("src", iconUrl)

                trEl.appendElement("td")
                    .id(MESSAGE_ELEMENT_ID)
                    .attr("style", "margin: 0; padding: 0;")
                    .text(message)
            } else {
                val iconUrl = if (isError) ICON_URL_TRANSLATION_FAILED else ICON_URL_TRANSLATION
                messageWrapperEl.selectFirst("#$ICON_ELEMENT_ID")?.attr("src", iconUrl)
                messageWrapperEl.selectFirst("#$MESSAGE_ELEMENT_ID")?.text(message)
            }
        }
    }
}
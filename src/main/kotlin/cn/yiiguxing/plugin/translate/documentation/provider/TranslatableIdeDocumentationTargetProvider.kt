package cn.yiiguxing.plugin.translate.documentation.provider

import cn.yiiguxing.plugin.translate.documentation.TranslatableDocumentationTarget
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.lang.documentation.ide.impl.IdeDocumentationTargetProviderImpl
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.psi.PsiFile


@Suppress("UnstableApiUsage")
class TranslatableIdeDocumentationTargetProvider(
    private val project: Project
) : IdeDocumentationTargetProvider {

    private val provider: IdeDocumentationTargetProvider by lazy {
        DocumentationTargetProviderFactory.createProvider(project)
    }

    override fun documentationTargets(
        editor: Editor,
        file: PsiFile,
        lookupElement: LookupElement
    ): List<DocumentationTarget> {
        return provider.documentationTargets(editor, file, lookupElement)
            .map { translatableDocumentationTarget(it, file.language) }
    }

    override fun documentationTarget(
        editor: Editor,
        file: PsiFile,
        lookupElement: LookupElement
    ): DocumentationTarget? {
        return provider.documentationTarget(editor, file, lookupElement)
            ?.let { translatableDocumentationTarget(it, file.language) }
    }

    override fun documentationTargets(editor: Editor, file: PsiFile, offset: Int): List<DocumentationTarget> {
        return wrapTargets(provider.documentationTargets(editor, file, offset), file.language)
    }

    private fun wrapTargets(targets: List<DocumentationTarget>, language: Language): List<DocumentationTarget> {
        return targets.map { translatableDocumentationTarget(it, language) }
    }

    private fun translatableDocumentationTarget(target: DocumentationTarget, language: Language): DocumentationTarget {
        return when {
            target is TranslatableDocumentationTarget -> target
            else -> TranslatableDocumentationTarget(project, language, target)
        }
    }
}


@Suppress("UnstableApiUsage")
private object DocumentationTargetProviderFactory {
    private const val RIDER_PRODUCT_CODE = "RD"
    private const val CLION_PRODUCT_CODE = "CL"
    private const val PLUGIN_ID_RADLER = "org.jetbrains.plugins.clion.radler"

    fun createProvider(project: Project): IdeDocumentationTargetProvider {
        val productCode = ApplicationInfo.getInstance().build.productCode
        return when (productCode) {
            RIDER_PRODUCT_CODE -> riderDocumentationTargetProvider(project)

            // In CLion, it actually uses Rider's documentation target provider, which is provided by
            // the "C++ Language Support via ReSharper" plugin (ID: org.jetbrains.plugins.clion.radler).
            CLION_PRODUCT_CODE -> if (PluginManagerCore.isPluginInstalled(PluginId.getId(PLUGIN_ID_RADLER))) {
                riderDocumentationTargetProvider(project)
            } else null

            else -> null
        } ?: defaultProvider(project)
    }

    private fun defaultProvider(project: Project): IdeDocumentationTargetProvider {
        return IdeDocumentationTargetProviderImpl(project)
    }

    private fun riderDocumentationTargetProvider(project: Project): IdeDocumentationTargetProvider? {
        return try {
            Class.forName("com.jetbrains.rider.quickDoc.RiderDocumentationTargetProvider")
                .getConstructor(Project::class.java)
                .newInstance(project) as IdeDocumentationTargetProvider
        } catch (e: Throwable) {
            thisLogger().error("Failed to create RiderDocumentationTargetProvider instance.", e)
            null
        }
    }
}
package cn.yiiguxing.plugin.translate.documentation

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.containers.ContainerUtil

@Service
internal class TranslatedDocComments: Disposable {

    //use SmartPsiElementPointer to survive reparse
    private val translatedDocs: MutableSet<SmartPsiElementPointer<PsiDocCommentBase>> = ContainerUtil.newConcurrentSet()

    override fun dispose() {
        translatedDocs.clear()
    }

    companion object {
        fun isTranslated(docComment: PsiDocCommentBase): Boolean {
            val translatedDocs = translatedDocs(docComment.project)
            return translatedDocs.contains(SmartPointerManager.createPointer(docComment))
        }

        fun setTranslated(docComment: PsiDocCommentBase, value: Boolean) {
            val translatedDocs = translatedDocs(docComment.project)

            val pointer = SmartPointerManager.createPointer(docComment)

            if (value) translatedDocs.add(pointer)
            else translatedDocs.remove(pointer)

            translatedDocs.scheduleCleanup()
        }

        private fun service(project: Project) = project.getService(TranslatedDocComments::class.java)

        private fun translatedDocs(project: Project) = service(project).translatedDocs

        private fun MutableSet<SmartPsiElementPointer<PsiDocCommentBase>>.scheduleCleanup() {
            ReadAction.nonBlocking {
                removeIf { it.element == null }
            }.submit(AppExecutorUtil.getAppExecutorService())
        }
    }

}
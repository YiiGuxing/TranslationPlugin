package cn.yiiguxing.plugin.translate.provider

import cn.yiiguxing.plugin.translate.util.startOffset
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

abstract class AbstractDocumentationElementProvider : DocumentationElementProvider {

    override fun findDocumentationElementAt(psiFile: PsiFile, offset: Int): PsiComment? {
        val offsetElement = psiFile.findElementAt(offset) ?: return null
        val comment = PsiTreeUtil.getParentOfType(offsetElement, PsiComment::class.java, false)
        val documentationElement: PsiComment? = if (
            comment == null // 如果当前元素或其父元素是注释元素，则跳过边缘拾取
            && offsetElement is PsiWhiteSpace
            && offsetElement.startOffset == offset // 光标处于边缘处
        ) {
            // 如果可在边缘拾取，则从末尾边缘处拾取
            (offsetElement.prevSibling as? PsiComment)?.takeIf { it.isPickAtEdge }
        } else comment

        return documentationElement?.takeIf { it.isDocComment && it.cachedOwner.owner != null }
    }

    /**
     * 检测目标注释是否是文档注释
     */
    protected abstract val PsiComment.isDocComment: Boolean

    /**
     * 目标注释是否可在边缘处拾取
     */
    protected open val PsiComment.isPickAtEdge: Boolean get() = false

    final override fun getDocumentationOwner(documentationElement: PsiElement): PsiElement? {
        return (documentationElement as? PsiComment)?.cachedOwner?.owner
    }

    /**
     * 缓存的注释所有者
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected val PsiComment.cachedOwner: CachedOwner
        get() {
            val modificationStamp = containingFile.modificationStamp
            return DOCUMENTATION_OWNER_CACHE[this@cachedOwner]
                ?.takeIf { it.isValid(modificationStamp) }
                ?: CachedOwner(if (isDocComment) documentationOwner else null, modificationStamp)
                    .also { DOCUMENTATION_OWNER_CACHE[this@cachedOwner] = it }
        }

    /**
     * 文档注释所有者
     */
    protected open val PsiComment.documentationOwner: PsiElement? get() = super.getDocumentationOwner(this)

    /**
     * 缓存的注释所有者
     *
     * @property owner 注释所有者
     * @property modificationStamp 修改标记
     *
     * @see PsiFile.getModificationStamp
     */
    protected data class CachedOwner(val owner: PsiElement?, val modificationStamp: Long) {
        /**
         * 通过指定的[修改标记][modificationStamp]检测当前缓存是否有效
         *
         * @see PsiFile.getModificationStamp
         */
        fun isValid(modificationStamp: Long): Boolean {
            return this.modificationStamp == modificationStamp && (owner?.isValid ?: true)
        }
    }

    protected companion object {
        val DOCUMENTATION_OWNER_CACHE = Key.create<CachedOwner?>("DOCUMENTATION_OWNER_CACHE")
    }

}
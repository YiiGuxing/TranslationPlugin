@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package cn.yiiguxing.plugin.translate.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.ui.components.JBLoadingPanelListener
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.LayoutManager
import javax.swing.JPanel

/**
 * Loading panel
 */
open class LoadingPanel(
    manager: LayoutManager? = null,
    onCreateLoadingDecorator: (JPanel) -> LoadingDecorator
) : JPanel(BorderLayout()) {

    val contentPanel: JPanel

    private lateinit var decorator: LoadingDecorator

    private val listeners: MutableCollection<JBLoadingPanelListener> = ContainerUtil.createLockFreeCopyOnWriteList()

    constructor(manager: LayoutManager?, parent: Disposable, startDelayMs: Int = -1)
            : this(manager, { panel: JPanel -> LoadingDecorator(panel, parent, startDelayMs) })

    init {
        contentPanel = manager?.let { JPanel(manager) } ?: JPanel()
        contentPanel.isOpaque = false
        contentPanel.isFocusable = false
        onLoadingDecoratorCreated(onCreateLoadingDecorator(contentPanel))
    }

    val isLoading: Boolean
        get() = decorator.isLoading

    private fun onLoadingDecoratorCreated(decorator: LoadingDecorator) {
        this.decorator = decorator
        super.add(decorator.component, BorderLayout.CENTER)
    }

    override fun setLayout(mgr: LayoutManager) {
        require(mgr is BorderLayout) { mgr.toString() }
        super.setLayout(mgr)
        if (::decorator.isInitialized) {
            super.add(decorator.component, BorderLayout.CENTER)
        }
    }

    fun setLoadingText(@Nls text: String?) {
        decorator.loadingText = text
    }

    fun stopLoading() {
        decorator.stopLoading()
        for (listener in listeners) {
            listener.onLoadingFinish()
        }
    }


    fun startLoading() {
        decorator.startLoading(false)
        for (listener in listeners) {
            listener.onLoadingStart()
        }
    }

    fun addListener(listener: JBLoadingPanelListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: JBLoadingPanelListener): Boolean {
        return listeners.remove(listener)
    }

    override fun add(comp: Component): Component {
        return contentPanel.add(comp)
    }

    override fun add(comp: Component, index: Int): Component {
        return contentPanel.add(comp, index)
    }

    override fun add(comp: Component, constraints: Any) {
        contentPanel.add(comp, constraints)
    }

    override fun getPreferredSize(): Dimension {
        return contentPanel.preferredSize
    }
}
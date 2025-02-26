package cn.yiiguxing.plugin.translate.trans.openai.prompt.template

import org.apache.velocity.exception.VelocityException

class TemplateRenderException(
    override val message: String,
    cause: VelocityException
) : RuntimeException(message, cause)
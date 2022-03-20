package cn.yiiguxing.plugin.translate.diagnostic.github

class TranslationGitHubAppException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)
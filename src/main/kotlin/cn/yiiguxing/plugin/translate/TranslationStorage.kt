package cn.yiiguxing.plugin.translate

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TranslationStorage {
    const val PREFERENCES_STORAGE_NAME = "yiiguxing.translation.xml"

    private const val DATA_DIRECTORY_NAME = "TranslationPlugin"
    private val USER_HOME_PATH: String = System.getProperty("user.home")
    private val DEFAULT_DATA_DIRECTORY = Paths.get(USER_HOME_PATH, ".$DATA_DIRECTORY_NAME")

    val DATA_DIRECTORY: Path = System.getenv(if (SystemInfo.isWindows) "LOCALAPPDATA" else "XDG_DATA_HOME")
        ?.takeIf { it.isNotEmpty() }
        ?.let { Paths.get(it, "Yii.Guxing", DATA_DIRECTORY_NAME) }
        ?: DEFAULT_DATA_DIRECTORY

    val CACHE_DIRECTORY: Path = DATA_DIRECTORY.resolve("caches")

    fun createDataDirectoriesIfNotExists() {
        if (!Files.exists(DATA_DIRECTORY) || !Files.isDirectory(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY)
        }
    }

    fun createCacheDirectoriesIfNotExists() {
        if (!Files.exists(CACHE_DIRECTORY) || !Files.isDirectory(CACHE_DIRECTORY)) {
            Files.createDirectories(CACHE_DIRECTORY)
        }
    }
}
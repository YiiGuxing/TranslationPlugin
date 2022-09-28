package cn.yiiguxing.plugin.translate

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Provides data storage locations.
 */
object TranslationStorages {
    /**
     * Storage name for persistent data such as preferences, state, etc.
     */
    const val PREFERENCES_STORAGE_NAME = "yiiguxing.translation.xml"

    private const val DATA_DIRECTORY_NAME = "TranslationPlugin"
    private val USER_HOME_PATH: String = System.getProperty("user.home")
    private val DEFAULT_DATA_DIRECTORY = Paths.get(USER_HOME_PATH, ".$DATA_DIRECTORY_NAME")

    /**
     * The data root directory of this plugin. Usually at the following locations:
     *
     * - Windows: "%LOCALAPPDATA%\Yii.Guxing\TranslationPlugin\"
     * - Other OS: "$XDG_DATA_HOME/Yii.Guxing/TranslationPlugin/" or "~/.TranslationPlugin/"
     */
    val DATA_DIRECTORY: Path = System.getenv(if (SystemInfo.isWindows) "LOCALAPPDATA" else "XDG_DATA_HOME")
        ?.takeIf { it.isNotEmpty() }
        ?.let { Paths.get(it, "Yii.Guxing", DATA_DIRECTORY_NAME) }
        ?: DEFAULT_DATA_DIRECTORY

    /**
     * The cache directory of this plugin, location: "${[DATA_DIRECTORY]}/caches/"
     */
    val CACHE_DIRECTORY: Path = DATA_DIRECTORY.resolve("caches")

    /**
     * Creates the data directory if it not exists,
     * and create all nonexistent parent directories first.
     */
    fun createDataDirectoriesIfNotExists() {
        if (!Files.exists(DATA_DIRECTORY) || !Files.isDirectory(DATA_DIRECTORY)) {
            Files.createDirectories(DATA_DIRECTORY)
        }
    }

    /**
     * Creates the cache directory if it not exists,
     * and create all nonexistent parent directories first.
     */
    fun createCacheDirectoriesIfNotExists() {
        if (!Files.exists(CACHE_DIRECTORY) || !Files.isDirectory(CACHE_DIRECTORY)) {
            Files.createDirectories(CACHE_DIRECTORY)
        }
    }
}
import org.apache.tools.ant.filters.EscapeUnicode
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}


val baseVersion = properties("version").get()
val isReleaseBuild = properties("release").map(String::toBoolean).getOrElse(false)
val snapshotId = properties("snapshotId").orNull

version = when {
    isReleaseBuild -> baseVersion
    else -> "$baseVersion-SNAPSHOT" + (snapshotId?.let { ".$it" } ?: "")
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.dbutils)
    implementation(libs.websocket) { exclude(module = "slf4j-api") }
    implementation(libs.mp3spi) { exclude(module = "junit") }
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension.
    // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        testFramework(TestFrameworkType.Platform)

        // Path to IDE distribution that will be used to run the IDE with the plugin.
        // local("path to IDE-dependency")
    }
}

// IntelliJ Platform Gradle Plugin Platform Extension.
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = properties("sinceBuild")
            untilBuild = properties("untilBuild").map { it.ifBlank { null } }
        }
    }

    publishing {
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel,
        // When using a non-default release channel, IntelliJ Platform-Based IDEs users will need to add a
        // new custom plugin repository to install your plugin from the specified channel. For example, if
        // specified 'snapshot' as a release channel, then users will need to add the
        // https://plugins.jetbrains.com/plugins/snapshot/list repository to install the plugin and receive updates.
        // These channels are treated as separate repositories for all intents and purposes. Read more:
        // https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html
        // Snapshot repositories:
        // https://plugins.jetbrains.com/plugins/snapshot/list
        // https://plugins.jetbrains.com/plugins/snapshot/8579
        channels = provider { listOf(getReleaseChannel(version.toString())) }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    changelog.header = provider { "${version.get()} (${date("yyyy/MM/dd")})" }
    groups.empty()
}

tasks {
    runIde {
        systemProperty("idea.log.trace.categories", project.group)
        systemProperty("idea.log.debug.categories", project.group)

        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                // Run the IDE in a specified language.
                // "-Duser.language=en"
            )
        }
    }

    val openApiSourceTask = registerOpenApiSourceTask()
    buildPlugin {
        dependsOn(openApiSourceTask)
        from(openApiSourceTask) { into("lib/src") }
    }

    processResources {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }

    withType<AbstractArchiveTask>().configureEach {
        if (name != openApiSourceTask.name) {
            archiveBaseName.set(project.name.toArchiveFileSegment())
        }
    }
}


fun properties(key: String): Provider<String> = providers.gradleProperty(key)

fun date(pattern: String): String =
    LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))

fun getReleaseChannel(version: String): String {
    val preRelease = version.substringBefore('+').substringAfter('-', "")
    return when {
        version.contains("-snapshot", ignoreCase = true) -> "snapshot"
        preRelease.isEmpty() -> "default"
        else -> preRelease.substringBefore('.').lowercase()
    }
}

fun registerOpenApiSourceTask(): TaskProvider<Jar> = tasks.register<Jar>("createOpenApiSourceJar") {
    description = "Create a source JAR for the OpenAPI module."
    // Kotlin source
    from(kotlin.sourceSets.main.get().kotlin) {
        include("**/cn/yiiguxing/plugin/translate/openapi/**/*.kt")
    }
    manifest {
        attributes(
            "Version" to properties("openApiVersion")
        )
    }

    includeEmptyDirs = false
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveBaseName.set("openapi")
    archiveVersion.set(provider { null })
    archiveClassifier.set("sources")
}

fun String.toArchiveFileSegment(): String = trim().lowercase().replace(Regex("\\s+"), "-")

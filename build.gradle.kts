import org.apache.tools.ant.filters.EscapeUnicode
import org.jetbrains.changelog.Changelog
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
}


fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String): String =
    LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))

val autoSnapshotVersionEnv: Provider<Boolean> = environment("AUTO_SNAPSHOT_VERSION").map(String::toBoolean).orElse(true)
val snapshotVersionPart: Provider<String> = properties("autoSnapshotVersion")
    .map(String::toBoolean)
    .orElse(false)
    .zip(autoSnapshotVersionEnv, Boolean::and)
    .map { if (it) "SNAPSHOT.${dateValue("yyMMdd")}" else "" }
val preReleaseVersion: Provider<String> = properties("pluginPreReleaseVersion")
    .flatMap { prv -> prv.takeIf(String::isNotBlank)?.let { provider { it } } ?: snapshotVersionPart }
val preReleaseVersionPart: Provider<String> = preReleaseVersion.map { prv ->
    prv.takeIf(String::isNotBlank)?.let { "-$it" } ?: ""
}
val buildMetadataPart: Provider<String> = properties("pluginBuildMetadata")
    .map { part -> part.takeIf(String::isNotBlank)?.let { "+$it" } ?: "" }
val pluginVersion: Provider<String> = properties("pluginMajorVersion").zip(preReleaseVersionPart, String::plus)
val fullPluginVersion: Provider<String> = pluginVersion.zip(buildMetadataPart, String::plus)

val versionRegex =
    Regex("""^((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)${'$'}""")
if (!versionRegex.matches(fullPluginVersion.get())) {
    throw GradleException("Plugin version '${fullPluginVersion.get()}' does not match the pattern '$versionRegex'")
}

val publishChannel: Provider<String> = preReleaseVersion.map { preReleaseVersion: String ->
    preReleaseVersion.takeIf(String::isNotEmpty)?.split(".")?.firstOrNull()?.lowercase() ?: "default"
}

extra["pluginVersion"] = pluginVersion.get()
extra["pluginPreReleaseVersion"] = preReleaseVersion.get()
extra["fullPluginVersion"] = fullPluginVersion.get()
extra["publishChannel"] = publishChannel.get()

group = properties("pluginGroup").get()
version = fullPluginVersion.get()

repositories {
    mavenLocal()
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.dbutils)
    implementation(libs.websocket) { exclude(module = "slf4j-api") }
    implementation(libs.mp3spi) { exclude(module = "junit") }
    testImplementation(libs.junit)
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Use `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    header = provider { "${version.get()} (${dateValue("yyyy/MM/dd")})" }
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml { onCheck = true }
            html { onCheck = true }
        }
    }
}

tasks {
    runIde {
        systemProperty("idea.is.internal", true)
        systemProperty("idea.log.trace.categories", "cn.yiiguxing.plugin.translate")
        systemProperty("idea.log.debug.categories", "cn.yiiguxing.plugin.translate")

        jvmArgs = listOf(
            // Run the IDE in a specified language.
            // "-Duser.language=en"
        )

        // Path to IDE distribution that will be used to run the IDE with the plugin.
        // ideDir.set(File("path to IDE-dependency"))
    }

    buildSearchableOptions {
        enabled = properties("intellij.buildSearchableOptions.enabled").map(String::toBoolean).getOrElse(true)
    }

    patchPluginXml {
        version = fullPluginVersion
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")
        pluginDescription = projectDir.resolve("DESCRIPTION.md").readText()

        // local variable for configuration cache compatibility
        val changelog = project.changelog
        // Get the latest available change notes from the changelog file
        changeNotes = pluginVersion.map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        }
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
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
        channels = publishChannel.map { listOf(it) }
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
        distributionType = Wrapper.DistributionType.ALL
    }

    val createOpenApiSourceJar by registering(Jar::class) {
        // Kotlin source
        from(kotlin.sourceSets.main.get().kotlin) {
            include("**/cn/yiiguxing/plugin/translate/openapi/**/*.kt")
        }
        manifest {
            attributes(
                "OpenApi-Version" to properties("openApiVersion")
            )
        }

        includeEmptyDirs = false
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        archiveClassifier.set("src")
    }

    buildPlugin {
        dependsOn(createOpenApiSourceJar)
        from(createOpenApiSourceJar) { into("lib/src") }
    }

    processResources {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }
}

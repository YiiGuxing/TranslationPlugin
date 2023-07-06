import org.apache.tools.ant.filters.EscapeUnicode
import org.jetbrains.changelog.Changelog
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.14.2"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.1.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.7.2"
}


fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String) = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))


val autoSnapshotVersionEnv = environment("AUTO_SNAPSHOT_VERSION").map(String::toBoolean).orElse(true)
val snapshotVersionPart = properties("autoSnapshotVersion")
    .map(String::toBoolean)
    .orElse(false)
    .zip(autoSnapshotVersionEnv) { isAutoSnapshotVersion, autoSnapshotVersionEnv ->
        isAutoSnapshotVersion && autoSnapshotVersionEnv
    }
    .map { if (it) "SNAPSHOT.${dateValue("yyMMdd")}" else null }
val preReleaseVersion = properties("pluginPreReleaseVersion")
    .map { it.takeIf(String::isNotBlank) }
    .orElse(snapshotVersionPart)
val buildMetadataPart = properties("pluginBuildMetadata")
    .map { it.takeIf(String::isNotBlank) }
    .map { "+$it" }
    .orElse("")
val pluginVersion = properties("pluginMajorVersion")
    .zip(preReleaseVersion.map { "-$it" }.orElse("")) { majorVersion, preReleaseVersion ->
        majorVersion + preReleaseVersion
    }
val fullPluginVersion = pluginVersion.zip(buildMetadataPart) { pluginVersion, buildMetadata ->
    pluginVersion + buildMetadata
}

val versionRegex =
    Regex("""^((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)${'$'}""")
if (!versionRegex.matches(fullPluginVersion.get())) {
    throw GradleException("Plugin version '${fullPluginVersion.get()}' does not match the pattern '$versionRegex'")
}

val publishChannel = preReleaseVersion.map { preReleaseVersion: String? ->
    preReleaseVersion?.split(".")?.firstOrNull()?.lowercase()
}.orElse("default")

extra["pluginVersion"] = pluginVersion.get()
extra["pluginPreReleaseVersion"] = preReleaseVersion.getOrElse("")
extra["fullPluginVersion"] = fullPluginVersion.get()
extra["publishChannel"] = publishChannel.get()

group = properties("pluginGroup").get()
version = fullPluginVersion.get()

repositories {
    mavenLocal()
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation("org.jsoup:jsoup:1.16.1")
    implementation("commons-dbutils:commons-dbutils:1.7")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
        exclude("junit")
    }
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain(11)
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Use `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    header.set(provider { "${version.get()} (${dateValue("yyyy/MM/dd")})" })
    groups.empty()
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(provider { file(".qodana").canonicalPath })
    reportPath.set(provider { file("build/reports/inspections").canonicalPath })
    saveReport.set(true)
    showReport.set(environment("QODANA_SHOW_REPORT").map(String::toBoolean).getOrElse(false))
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml { onCheck = true }
        html { onCheck = true }
    }
}

tasks {
    runIde {
        systemProperty("idea.is.internal", true)
        systemProperty("translation.plugin.log.stdout", true)

        jbrVariant.set("dcevm")
        // Enable hotswap, requires JBR 17+ or JBR 11 with DCEVM, and run in debug mode.
        jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition")

        // Path to IDE distribution that will be used to run the IDE with the plugin.
        // ideDir.set(File("path to IDE-dependency"))
    }

    buildSearchableOptions {
        enabled = properties("intellij.buildSearchableOptions.enabled").map(String::toBoolean).getOrElse(true)
    }

    patchPluginXml {
        version.set(fullPluginVersion)
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
        pluginDescription.set(projectDir.resolve("DESCRIPTION.md").readText())

        // local variable for configuration cache compatibility
        val changelog = project.changelog
        // Get the latest available change notes from the changelog file
        changeNotes.set(pluginVersion.map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML
                )
            }
        })
    }

    signPlugin {
        certificateChain.set(environment("CERTIFICATE_CHAIN"))
        privateKey.set(environment("PRIVATE_KEY"))
        password.set(environment("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(environment("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel,
        // When using a non-default release channel, IntelliJ Platform Based IDEs users will need to add a
        // new custom plugin repository to install your plugin from the specified channel. For example, if
        // specified 'snapshot' as a release channel, then users will need to add the
        // https://plugins.jetbrains.com/plugins/snapshot/list repository to install the plugin and receive updates.
        // These channels are treated as separate repositories for all intents and purposes. Read more:
        // https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html
        // Snapshot repositories:
        // https://plugins.jetbrains.com/plugins/snapshot/list
        // https://plugins.jetbrains.com/plugins/snapshot/8579
        channels.set(publishChannel.map { listOf(it) })
    }

    wrapper {
        gradleVersion = properties("gradleVersion").get()
        distributionType = Wrapper.DistributionType.ALL
    }

    processResources {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }
}

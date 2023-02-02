import org.apache.tools.ant.filters.EscapeUnicode
import org.jetbrains.changelog.Changelog
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.12.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"
}


fun properties(key: String) = project.findProperty(key).toString()
fun dateValue(pattern: String) = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))


val pluginMajorVersion: String by project
val pluginPreReleaseVersion: String by project
val pluginBuildMetadata: String by project
val preReleaseVersion: String? = pluginPreReleaseVersion
    .takeIf { it.isNotBlank() }
    ?: "SNAPSHOT.${dateValue("yyMMdd")}".takeIf {
        properties("autoSnapshotVersion").toBoolean()
                && !"false".equals(System.getenv("AUTO_SNAPSHOT_VERSION"), ignoreCase = true)
    }
val preReleaseVersionPart = preReleaseVersion?.let { "-$it" } ?: ""
val buildMetadataPart = pluginBuildMetadata.takeIf { it.isNotBlank() }?.let { "+$it" } ?: ""
val pluginVersion = pluginMajorVersion + preReleaseVersionPart
val fullPluginVersion = pluginVersion + buildMetadataPart

val versionRegex =
    Regex("""^((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)${'$'}""")
if (!versionRegex.matches(fullPluginVersion)) {
    throw GradleException("Plugin version '$fullPluginVersion' does not match the pattern '$versionRegex'")
}

val publishChannel = preReleaseVersion?.split(".")?.firstOrNull()?.toLowerCase() ?: "default"

extra["pluginVersion"] = pluginVersion
extra["pluginPreReleaseVersion"] = preReleaseVersion ?: ""
extra["fullPluginVersion"] = fullPluginVersion
extra["publishChannel"] = publishChannel

group = properties("pluginGroup")
version = fullPluginVersion

repositories {
    mavenLocal()
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    testImplementation("junit:junit:4.13.2")

    implementation("org.jsoup:jsoup:1.15.3")
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
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    header.set(provider { "${version.get()} (${dateValue("yyyy/MM/dd")})" })
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(file(".qodana").canonicalPath)
    reportPath.set(file("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    xmlReport {
        onCheck.set(true)
    }
    htmlReport {
        onCheck.set(true)
    }
}

tasks {
    runIde {
        systemProperty("idea.is.internal", true)
        systemProperty("translation.plugin.log.stdout", true)

        // Path to IDE distribution that will be used to run the IDE with the plugin.
        // ideDir.set(File("path to IDE-dependency"))
    }

    buildSearchableOptions {
        enabled = !"false".equals(properties("intellij.buildSearchableOptions.enabled"), ignoreCase = true)
    }

    patchPluginXml {
        version.set(fullPluginVersion)
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
        pluginDescription.set(projectDir.resolve("DESCRIPTION.md").readText())

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            with(changelog) {
                renderItem((getOrNull(pluginVersion) ?: getUnreleased()).withHeader(false), Changelog.OutputType.HTML)
            }
        })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
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
        channels.set(listOf(publishChannel))
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionType = Wrapper.DistributionType.ALL
    }

    processResources {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }
}

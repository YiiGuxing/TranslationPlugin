import org.apache.tools.ant.filters.EscapeUnicode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.6.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
}


fun properties(key: String) = project.findProperty(key).toString()
fun dateValue(pattern: String) = LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))


val pluginMajorVersion: String by project
val pluginPreReleaseVersion: String by project
val pluginBuildMetadata: String by project
val preReleaseVersion = pluginPreReleaseVersion
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
    Regex("""^v((0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?)${'$'}""")
if (!versionRegex.matches("v$fullPluginVersion")) {
    throw GradleException("Plugin version 'v$fullPluginVersion' does not match the pattern '$versionRegex'")
}

val publishChannel = preReleaseVersion?.split(".")?.firstOrNull()?.toLowerCase() ?: "default"

extra["pluginVersion"] = pluginVersion
extra["pluginPreReleaseVersion"] = preReleaseVersion
extra["fullPluginVersion"] = fullPluginVersion
extra["publishChannel"] = publishChannel

group = properties("pluginGroup")
version = fullPluginVersion

repositories {
    mavenLocal()
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://maven-central.storage-download.googleapis.com/repos/central/data/")
    maven(url = "https://repo.eclipse.org/content/groups/releases/")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    implementation("org.jsoup:jsoup:1.15.2")
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("commons-dbutils:commons-dbutils:1.7")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
        exclude("junit")
    }

    compileOnly("org.xerial:sqlite-jdbc:3.36.0.3")
}

// Set the JVM language level used to compile sources and generate files - Java 11 is required since 2020.3
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(pluginVersion)
    header.set(provider { "v${version.get()} (${dateValue("yyyy/MM/dd")})" })
    headerParserRegex.set(versionRegex)
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

tasks {
    runIde {
        systemProperties["idea.is.internal"] = true

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
            changelog.run {
                getOrNull(pluginVersion) ?: getUnreleased()
            }.toHTML()
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
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
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

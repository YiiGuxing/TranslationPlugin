import org.apache.tools.ant.filters.EscapeUnicode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.4.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.1"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
}

val pluginMajorVersion: String by project
val pluginVariantVersion: String by project
val variantVersionPart = pluginVariantVersion.takeIf { it.isNotBlank() }?.let { "-$it" } ?: ""
val isSnapshot = !"false".equals(System.getenv("SNAPSHOT_VERSION"), ignoreCase = true)
val snapshotPart = if (isSnapshot) "-SNAPSHOT" else ""
val pluginVersion = "$pluginMajorVersion$variantVersionPart"
val fullPluginVersion = "$pluginVersion$snapshotPart"

extra["pluginVersion"] = pluginVersion
extra["fullPluginVersion"] = fullPluginVersion

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

    implementation("org.jsoup:jsoup:1.14.3")
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("commons-dbutils:commons-dbutils:1.7")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
        exclude("junit")
    }
}

// Set the JVM language level used to compile sources and generate files - Java 11 is required since 2020.3
kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(11))
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
    val date = LocalDate.now(ZoneId.of("Asia/Shanghai"))
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    version.set(pluginVersion)
    header.set(provider { "v${version.get()} (${date.format(formatter)})" })
    headerParserRegex.set(Regex("v(\\d(\\.\\d+)+(-([0-9a-zA-Z]+(\\.[0-9a-zA-Z]+)*))?)"))
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
        // channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionType = Wrapper.DistributionType.ALL
    }

    withType<ProcessResources> {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }
}

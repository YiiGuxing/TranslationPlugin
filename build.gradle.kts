
import org.apache.tools.ant.filters.EscapeUnicode
import org.jetbrains.changelog.date
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.1.6"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "1.3.0"
}

val fullPluginVersion = properties("pluginVersion").let { pluginVersion ->
    val variantPart = properties("pluginVariantVersion").let { if (it.isNotEmpty()) "-$it" else "" }
    val snapshotPart = if (properties("isSnapshot").toBoolean()) "-SNAPSHOT" else ""
    "$pluginVersion$variantPart$snapshotPart"
}

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
    compileOnly(fileTree("libs"))
    testImplementation("junit:junit:4.13.2")

    implementation("org.jsoup:jsoup:1.14.2")
    implementation("org.apache.commons:commons-dbcp2:2.8.0")
    implementation("commons-dbutils:commons-dbutils:1.7")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") {
        exclude("junit")
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(true)

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    header.set(provider { "v${version.get()} (${date("yyyy/MM/dd")})" })
    headerParserRegex.set(Regex("v\\d(\\.\\d+)+"))
    keepUnreleasedSection.set(false)
    groups.set(emptyList())
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    runIde {
        systemProperties["idea.auto.reload.plugins"] = false
    }

    patchPluginXml {
        version.set(fullPluginVersion)
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))
        pluginDescription.set(projectDir.resolve("DESCRIPTION.md").readText())

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            val changelogUrl = "https://github.com/YiiGuxing/TranslationPlugin/blob/master/CHANGELOG.md"
            changelog.run {
                getOrNull("v${properties("pluginVersion")}") ?: getLatest()
            }.toHTML() + "<br/><a href=\"$changelogUrl\"><b>Full Changelog History</b></a>"
        })
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
        distributionType = Wrapper.DistributionType.ALL
    }

    withType<KotlinCompile> {
        kotlinOptions.languageVersion = properties("kotlinLanguageVersion")
        kotlinOptions.apiVersion = properties("kotlinTargetVersion")
        kotlinOptions.jvmTarget = properties("javaVersion")
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<ProcessResources> {
        filesMatching("**/*.properties") {
            filter(EscapeUnicode::class)
        }
    }
}

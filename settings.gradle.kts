import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "IntelliJ Translation Plugin"

pluginManagement {
    plugins {
        // Warning: Kotlin plugin 2.2.x is incompatible and will result in a startup crash
        // (NoClassDefFoundError: kotlin/coroutines/jvm/internal/SpillingKt in ProjectActivity).
        // Please upgrade only when compiling against a compatible version of IDEA.
        id("org.jetbrains.kotlin.jvm") version "2.1.21"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        maven("https://maven.aliyun.com/repository/public/")
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension.
        // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}

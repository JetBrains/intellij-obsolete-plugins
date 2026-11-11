import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2024.3.5")
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.intellij.play"
        name = "Play Framework"
        version = "2024.3.5"
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "243.*"
        }
    }
}
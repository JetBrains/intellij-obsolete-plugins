// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "org.jetbrains.aidebugger"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(providers.gradleProperty("platformJavaVersion").get().toInt())
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.PyCharmProfessional, providers.gradleProperty("platformVersion"))
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
    }
}

sourceSets {
    main {
        java.srcDirs(
            "ai-debugger-common/src",
            "ai-debugger-koog/src",
            "ai-debugger-python/src",
            "evaluation/src",
            "evaluation-cli/src"
        )
        resources.srcDirs(
            "src/main/resources",
            "ai-debugger-common/resources",
            "ai-debugger-koog/resources",
            "ai-debugger-python/resources",
            "evaluation/resources",
            "evaluation-cli/resources"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "AI Agents Debugger"
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = providers.gradleProperty("platformJavaVersion").get()
    targetCompatibility = providers.gradleProperty("platformJavaVersion").get()
}

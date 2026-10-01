// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.intellij.aiplayground"
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
        bundledPlugin("com.intellij.modules.ultimate")
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
    }
}

sourceSets {
    main {
        java.srcDirs(
            "src",
            "aiassistant/src",
            "anthropic/src",
            "deepseek/src",
            "gemini/src",
            "langchain/src",
            "mistral/src",
            "models/src",
            "ollama/src",
            "openai/src",
            "openrouter/src",
            "python/src",
            "settings/src",
            "ui/src"
        )
        resources.srcDirs(
            "resources",
            "aiassistant/resources",
            "anthropic/resources",
            "deepseek/resources",
            "gemini/resources",
            "langchain/resources",
            "mistral/resources",
            "models/resources",
            "ollama/resources",
            "openai/resources",
            "openrouter/resources",
            "python/resources",
            "settings/resources",
            "ui/resources"
        )
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "AI Playground"
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

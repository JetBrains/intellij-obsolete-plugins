// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.intellij.python.huggingFace"
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
        create(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.PyCharmProfessional, providers.gradleProperty("platformVersion"))
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
        bundledPlugin("com.intellij.notebooks.core")
        bundledPlugin("intellij.jupyter")
    }
}

sourceSets {
    main {
        java.srcDirs("src", "gen")
        resources.srcDirs("resources")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Hugging Face"
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

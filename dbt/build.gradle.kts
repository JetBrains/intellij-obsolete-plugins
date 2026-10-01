// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "org.jetbrains.dbt"
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
        bundledPlugin("com.intellij.database")
        bundledPlugin("com.intellij.diagram")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.libraries.jackson.module.kotlin")
        bundledPlugin("com.intellij.modules.ultimate")
        bundledPlugin("PythonCore")
        bundledPlugin("Pythonid")
    }
}

sourceSets {
    main {
        java.srcDirs("src", "gen", "python/src")
        resources.srcDirs("resources", "python/resources")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "dbt Support"
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

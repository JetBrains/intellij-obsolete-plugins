import org.gradle.api.internal.tasks.JvmConstants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform.module")
}

group = "org.intellij.grails.testFramework"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("org.jetbrains.plugins.gradle")

        // add to _production_ deps
        testFramework(
            type = TestFrameworkType.Plugin.Java,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Platform,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Plugin.Maven,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Plugin.ExternalSystem,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )

    }
    compileOnly("junit:junit:4.13.2")
    compileOnly("org.assertj:assertj-core:4.0.0-M1")

    compileOnly(project(":"))
}

java.sourceSets["main"].java {
    srcDir("src")
}

tasks {
        withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

}

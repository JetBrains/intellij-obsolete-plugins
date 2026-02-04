// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.osgi"
version = "253.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")

        testBundledPlugin("org.intellij.groovy")
        testBundledPlugin("com.jetbrains.performancePlugin.async") // todo??
        testFramework(TestFrameworkType.Metrics)
//        testFramework(TestFrameworkType.Plugin.ExternalSystem) // todo ??
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
        testFramework(TestFrameworkType.Platform)
    }

    implementation("org.osgi:org.osgi.namespace.contract:1.0.0")
    implementation("org.osgi:org.osgi.namespace.extender:1.0.1")
    implementation("org.osgi:org.osgi.namespace.implementation:1.0.0")
    implementation("org.osgi:org.osgi.namespace.service:1.0.0")

    implementation("biz.aQute.bnd:biz.aQute.bndlib:5.3.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("biz.aQute.bnd:biz.aQute.repository:5.3.0") {
        exclude(group = "biz.aQute.bnd", module = "biz.aQute.bndlib")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("biz.aQute.bnd:biz.aQute.resolve:5.3.0") {
        exclude(group = "biz.aQute.bnd", module = "biz.aQute.bndlib")
        exclude(group = "biz.aQute.bnd", module = "biz.aQute.repository")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")

    implementation(project(":osmorc-jps-plugin"))
}

kotlin {
    jvmToolchain(21)
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["main"].resources {
    srcDir("resources")
    srcDir("compatibilityResources")
}

java.sourceSets["test"].java {
    srcDir("test")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }

        changeNotes = ""
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // https://plugins.jetbrains.com/docs/intellij/testing-faq.html#how-to-test-a-jvm-language
    // set path to local intellij-community project
    test {
        systemProperty("idea.home.path", "/Users/yann/idea-ultimate/ultimate/community")
    }
}

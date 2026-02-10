// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.gradle.internal.classpath.Instrumented.systemProperty
import org.gradle.internal.impldep.org.eclipse.jgit.diff.DiffDriver
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.helidon"
version = "253.0.0"

kotlin {
  jvmToolchain(21)
}

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
    bundledPlugin("com.intellij.modules.ultimate")
    bundledPlugin("com.intellij.cdi")
    bundledPlugin("tanvd.grazi")
    bundledPlugin("com.intellij.properties")
    bundledPlugin("com.intellij.modules.json")
    bundledPlugin("org.jetbrains.idea.maven")
    bundledPlugin("org.jetbrains.plugins.yaml")
    bundledPlugin("com.intellij.microservices.jvm")

    testBundledPlugin("com.intellij.java-i18n")
    testBundledModule("intellij.javaee.platform")
    testBundledModule("intellij.javaee.platform.impl")
    testBundledModule("intellij.javaee.web")
    testBundledModule("intellij.javaee.ejb")
    testBundledModule("intellij.javaee.application")
    testBundledModule("intellij.javaee.app.servers")
    testBundledModule("intellij.javaee.platform")

    testFramework(TestFrameworkType.Plugin.Java)
    testFramework(TestFrameworkType.Platform)
  }
  testImplementation("junit:junit:4.13.2")
}

DiffDriver.java.sourceSets["main"].java {
  srcDir("src")
  srcDir("gen")
}

DiffDriver.java.sourceSets["main"].resources {
  srcDir("resources")
}

DiffDriver.java.sourceSets["test"].java {
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




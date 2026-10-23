// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.intellij.properties.bundle.editor"
version = "262.0.0"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdea("2026.2.3")
    bundledPlugin("com.intellij.properties")
    bundledPlugin("intellij.structureView.plugin")
    // the plugins below are not used directly, they are required by the Properties plugin at runtime
    bundledPlugin("intellij.structuralSearch.plugin")
    bundledPlugin("com.intellij.copyright")
    bundledPlugin("intellij.libraries.misc.plugin")

    testFramework(TestFrameworkType.Platform)
  }

  testImplementation("junit:junit:4.13.2")
}

kotlin {
  jvmToolchain(25)
}

java.sourceSets["main"].java {
  srcDir("src")
}

java.sourceSets["main"].resources {
  srcDir("resources")
}

java.sourceSets["test"].java {
  srcDir("test")
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = "262"
    }

    changeNotes = "Build for 2026.2 IntelliJ IDEA"
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "25"
    targetCompatibility = "25"
  }
}

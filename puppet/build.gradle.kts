// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.lang.puppet"
version = "261.0.0"

kotlin {
  jvmToolchain(21)
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

// Puppet is a language plugin that depends only on the IntelliJ Platform plus the
// bundled RegExp, JSON and YAML support. IntelliJ IDEA Community is no longer published
// since 2025.3, so it is built against IntelliJ IDEA Ultimate.
dependencies {
  intellijPlatform {
    create(IntelliJPlatformType.IntellijIdeaUltimate, "2026.1.4")
    bundledModule("intellij.regexp")
    bundledPlugin("com.intellij.modules.json")
    bundledPlugin("org.jetbrains.plugins.yaml")
  }
}

java.sourceSets["main"].java {
  srcDir("src")
  srcDir("gen")
}

java.sourceSets["main"].resources {
  srcDir("resources")
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = "261"
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

  // The Puppet built-in stubs (builtin.pp, stub_functions.rb, ...) are loaded at runtime from
  // "<plugins>/puppet/lib/stubs/", so they must be shipped inside the plugin distribution.
  prepareSandbox {
    from("lib") {
      into("puppet/lib")
    }
  }
}

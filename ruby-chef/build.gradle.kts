// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "org.jetbrains.plugins.ruby.chef"
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

// Chef depends on RubyMine internals (org.jetbrains.plugins.ruby.*), so the plugin
// is built against the RubyMine distribution rather than IntelliJ IDEA.
dependencies {
  intellijPlatform {
    create(IntelliJPlatformType.RubyMine, "2026.1.4")
    bundledPlugin("com.intellij.modules.ultimate")
    bundledPlugin("org.jetbrains.plugins.ruby")
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
}

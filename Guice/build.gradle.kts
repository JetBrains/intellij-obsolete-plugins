// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  id("java")
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.guice"
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
    bundledPlugin("com.intellij.properties")
    bundledPlugin("com.intellij.modules.ultimate")
  }
}

java.sourceSets["main"].java {
  srcDir("gen")
  srcDir("src")
}

java.sourceSets["main"].resources {
  srcDir("resources")
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
}

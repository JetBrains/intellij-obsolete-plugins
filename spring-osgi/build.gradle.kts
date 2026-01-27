// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.spring.osgi"
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
    plugin("com.intellij.spring", "253.29346.138") // must match intellijIdea version
    bundledPlugin("com.intellij.java")
    bundledPlugin("com.intellij.modules.ultimate")

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Bundled)
  }
  testImplementation("junit:junit:4.13.2")
}

java.sourceSets["main"].java {
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

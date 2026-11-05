// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.19.0"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
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
    intellijIdea("2026.1")
    bundledPlugin("com.intellij.java")
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Plugin.Java)
  }
  testImplementation("com.google.truth:truth:1.4.2")
  // The tests add these jars as libraries to the test project. The plugin does not bundle them.
  testImplementation("com.google.inject:guice:6.0.0")
  testImplementation("com.google.inject.extensions:guice-assistedinject:6.0.0")
  testImplementation("jakarta.inject:jakarta.inject-api:2.0.1")
}

java {
  sourceSets.getByName("main") {
    java {
      srcDir("gen")
      srcDir("src")
    }
    kotlin {
      srcDir("gen")
      srcDir("src")
    }
    resources {
      srcDir("resources")
    }
  }
  sourceSets.getByName("test") {
    java {
      srcDir("test")
    }
    kotlin {
      srcDir("test")
    }
  }
}

kotlin {
  jvmToolchain(21)
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
    options.release = 21
  }
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }
}

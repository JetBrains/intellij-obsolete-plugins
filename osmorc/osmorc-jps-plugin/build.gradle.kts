// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.intellij.osgi"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

intellijPlatform {
  buildSearchableOptions = false
}

dependencies {
  intellijPlatform {
    intellijIdea("2025.3.1")
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.idea.maven")

    testFramework(TestFrameworkType.Plugin.Java)
    testFramework(TestFrameworkType.Plugin.ExternalSystem) // todo ??
    testFramework(TestFrameworkType.Platform)
  }

  implementation(files("../lib/bundlor-all.jar"))
  implementation("biz.aQute.bnd:biz.aQute.bndlib:5.3.0") {
    exclude(group="org.slf4j",module="slf4j-api")
  }

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.assertj:assertj-core:4.0.0-M1")
}

kotlin {
  jvmToolchain(21)
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

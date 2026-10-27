// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.intellij.properties.bundle.editor"
version = "262.0.1"

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

    changeNotes = """
      <ul>
        <li>Fixed lost and misdirected edits</li>
        <li>Fixed threading errors</li>
        <li>The order of keys is now computed in the background, keeping the UI responsive (IDEA-395129)</li>
        <li>Undo now reverts unsaved and saved edits immediately and in editing order (IDEA-395149)</li>
        <li>Pending edits are now written when documents are saved (IDEA-395112)</li>
        <li>The edited property now stays selected when undoing a saved edit (IDEA-395212)</li>
      </ul>
    """.trimIndent()
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "25"
    targetCompatibility = "25"
  }
}

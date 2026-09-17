// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform.module")
}

base {
  archivesName = "bigdatatools-plugin-spark-gradle-tooling"
}

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaUltimate(providers.gradleProperty("platformVersion"))
    bundledPlugin("com.intellij.gradle")
  }

  compileOnly(gradleApi())
}

kotlin {
  jvmToolchain(providers.gradleProperty("platformJavaVersion").get().toInt())
  compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

sourceSets {
  main {
    java.srcDir("src")
    resources.srcDir("resources")
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_1_8.toString()
  targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

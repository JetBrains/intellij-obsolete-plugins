// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

import org.jetbrains.intellij.platform.gradle.tasks.ComposedJarTask

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform.module")
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

    bundledPlugin("com.intellij.modules.json")

    plugin(
      "com.intellij.bigdatatools.core",
      providers.gradleProperty("bigDataToolsCoreVersion").get(),
    )
    plugin(
      "intellij.bigdatatools.coreUi",
      providers.gradleProperty("bigDataToolsCoreUiVersion").get(),
    )
    plugin(
      "intellij.bigdatatools.awsBase",
      providers.gradleProperty("bigDataToolsAwsBaseVersion").get(),
    )
    plugin(
      "intellij.bigdatatools.gcloud",
      providers.gradleProperty("bigDataToolsGoogleCloudVersion").get(),
    )
    plugin(
      "com.intellij.bigdatatools.rfs",
      providers.gradleProperty("remoteFileSystemsVersion").get(),
    )
  }

  implementation("org.apache.hive:hive-metastore:3.1.3") {
    isTransitive = false
  }
  implementation("org.apache.hive:hive-standalone-metastore:3.1.3") {
    isTransitive = false
  }
  implementation("io.dropwizard.metrics:metrics-jvm:3.1.0") {
    isTransitive = false
  }
  implementation("io.dropwizard.metrics:metrics-json:3.1.0") {
    isTransitive = false
  }
  implementation("org.apache.hive:hive-storage-api:2.7.0") {
    isTransitive = false
  }
  implementation("org.apache.logging.log4j:log4j-core:2.17.1") {
    isTransitive = false
  }
  implementation("org.datanucleus:datanucleus-api-jdo:4.2.4") {
    isTransitive = false
  }
  implementation("org.datanucleus:datanucleus-core:4.1.17") {
    isTransitive = false
  }
  implementation("org.datanucleus:datanucleus-rdbms:4.1.19") {
    isTransitive = false
  }
  implementation("org.apache.thrift:libfb303:0.9.3") {
    isTransitive = false
  }
  implementation("org.apache.thrift:libthrift:0.9.3") {
    isTransitive = false
  }

  compileOnly("org.xerial.snappy:snappy-java:1.1.10.7")
}

kotlin {
  jvmToolchain(providers.gradleProperty("platformJavaVersion").get().toInt())
}

sourceSets {
  main {
    java.srcDirs("src", "gen")
    resources.srcDir("resources")
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = providers.gradleProperty("platformJavaVersion").get()
  targetCompatibility = providers.gradleProperty("platformJavaVersion").get()
}

tasks.named<ComposedJarTask>("composedJar") {
  archiveBaseName = "intellij.bigdatatools.hiveMetastore"
}

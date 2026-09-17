// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
}

base {
  archivesName = "bigdatatools-plugin-spark"
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

    bundledPlugin("com.intellij.diagram")
    bundledPlugin("com.intellij.gradle")
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.idea.maven")
    bundledPlugin("org.jetbrains.plugins.gradle")
    bundledPlugin("intellij.ssh.plugin")

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
      "intellij.bigdatatools.azure",
      providers.gradleProperty("bigDataToolsAzureVersion").get(),
    )
    plugin(
      "intellij.bigdatatools.gcloud",
      providers.gradleProperty("bigDataToolsGoogleCloudVersion").get(),
    )
    plugin(
      "com.intellij.bigdatatools.rfs",
      providers.gradleProperty("remoteFileSystemsVersion").get(),
    )
    plugin(
      "PythonCore",
      providers.gradleProperty("pythonVersion").get(),
    )
    plugin(
      "Pythonid",
      providers.gradleProperty("pythonVersion").get(),
    )
    plugin(
      "org.intellij.scala",
      providers.gradleProperty("scalaVersion").get(),
    )
    plugin(
      "com.intellij.completion.ml.ranking",
      providers.gradleProperty("completionMlRankingVersion").get(),
    )

    localPlugin(project(":binaryFilesSupport"))
    localPlugin(project(":metastoreCore"))
  }

  implementation(project(":sparkGradleTooling"))
}

kotlin {
  jvmToolchain(providers.gradleProperty("platformJavaVersion").get().toInt())
}

sourceSets {
  main {
    java.srcDirs(
      "src",
      "gen",
      "../spark-assistance/src",
      "../spark-gradle/src",
      "../spark-java/src",
      "../spark-maven/src",
      "../spark-python/src",
      "../spark-scala/src",
      "../spark-scala/gen",
      "../../project-wizard/src",
      "../../spark-monitoring/src",
      "../../spark-monitoring/gen",
    )
    resources.srcDirs(
      "resources",
      "../spark-assistance/resources",
      "../spark-gradle/resources",
      "../spark-java/resources",
      "../spark-maven/resources",
      "../spark-python/resources",
      "../spark-scala/resources",
      "../../project-wizard/resources",
      "../../spark-monitoring/resources",
    )
  }
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = providers.gradleProperty("pluginSinceBuild")
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = providers.gradleProperty("platformJavaVersion").get()
  targetCompatibility = providers.gradleProperty("platformJavaVersion").get()
}

tasks.processResources {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
}

base {
  archivesName = "bigdatatools-zeppelin"
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

    bundledModule("intellij.charts")
    bundledModule("intellij.grid.csv.core.impl")
    bundledModule("intellij.grid.core.impl")
    bundledModule("intellij.grid.impl")
    bundledModule("intellij.libraries.okhttp")
    bundledModule("intellij.libraries.squareup.okio.jvm")
    bundledPlugin("com.intellij.database")
    bundledPlugin("com.intellij.java")
    bundledPlugin("com.intellij.modules.jcef")
    bundledPlugin("com.jetbrains.sh")
    bundledPlugin("org.intellij.plugins.markdown")
    bundledPlugin("org.jetbrains.idea.maven")

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
      "com.intellij.completion.ml.ranking",
      providers.gradleProperty("completionMlRankingVersion").get(),
    )
    plugin(
      "org.intellij.scala",
      providers.gradleProperty("scalaVersion").get(),
    )

    localPlugin(project(":binaryFilesSupport"))
    localPlugin(project(":metastoreCore"))
    localPlugin(project(":spark"))
  }

  implementation(
    "com.neovisionaries:nv-websocket-client:${providers.gradleProperty("nvWebsocketClientVersion").get()}",
  ) {
    isTransitive = false
  }
  implementation(
    "org.bitbucket.cowwoc:diff-match-patch:${providers.gradleProperty("diffMatchPatchVersion").get()}",
  ) {
    isTransitive = false
  }
}

kotlin {
  jvmToolchain(providers.gradleProperty("platformJavaVersion").get().toInt())
}

sourceSets {
  main {
    java.srcDirs(
      "src",
      "gen",
      "../jupyter/src",
      "../jupyter/gen",
      "../visualisation/src",
      "../visualisation/gen",
      "frontend/src",
    )
    resources.srcDirs(
      "resources",
      "../jupyter/resources",
      "../visualisation/resources",
      "frontend/resources",
      "libraries/resources",
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

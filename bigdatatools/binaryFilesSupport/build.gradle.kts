// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
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
  }
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

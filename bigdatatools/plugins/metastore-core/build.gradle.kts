// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  java
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij.platform")
}

base {
  archivesName = "bigdatatools-metastore-core"
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

    bundledPlugin("com.intellij.java")
    bundledPlugin("com.intellij.modules.json")
    bundledPlugin("com.jetbrains.plugins.webDeployment")
    bundledPlugin("intellij.ssh.plugin")
    bundledPlugin("org.jetbrains.plugins.terminal")
    bundledModule("intellij.libraries.protobuf.java.util")

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

    localPlugin(project(":binaryFilesSupport"))
    pluginModule(project(":hiveMetastore"))
  }

  implementation("com.google.cloud:google-cloud-dataproc:4.11.0") {
    isTransitive = false
  }
  implementation("com.google.auto.value:auto-value:1.10.1") {
    isTransitive = false
  }
  implementation("com.google.api.grpc:proto-google-cloud-dataproc-v1:4.11.0") {
    isTransitive = false
  }
  implementation("org.xerial.snappy:snappy-java:1.1.10.7") {
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
      "../../dataproc/src",
      "../../dataproc/gen",
      "../../emr/src",
      "../../emr/gen",
      "../../glue/src",
      "../../glue/gen",
      "../../hadoop-monitoring/src",
      "../../hadoop-monitoring/gen",
      "../../spark-sumbit/src",
      "../../spark-sumbit/gen",
    )
    resources.srcDirs(
      "resources",
      "../../dataproc/resources",
      "../../emr/resources",
      "../../glue/resources",
      "../../hadoop-monitoring/resources",
      "../../spark-sumbit/resources",
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

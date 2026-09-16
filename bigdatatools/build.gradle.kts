// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

plugins {
  base
  id("org.jetbrains.kotlin.jvm") version "2.3.0" apply false
  id("org.jetbrains.intellij.platform") version "2.19.0" apply false
}

allprojects {
  group = "com.intellij.bigdatatools"
  version = providers.gradleProperty("pluginVersion").get()
}

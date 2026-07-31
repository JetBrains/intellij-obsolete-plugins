// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
plugins {
    id("java")
}

group = "com.intellij.gwt.rt"

repositories {
    mavenCentral()
}

java.sourceSets["main"].java {
    srcDir("src")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}

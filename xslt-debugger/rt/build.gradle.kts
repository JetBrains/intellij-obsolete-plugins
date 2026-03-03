// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
}

group = "org.intellij.xsltDebugger.rt"

java.sourceSets["main"].java {
    srcDir("src")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }

    jar {
        archiveFileName = "xslt-debugger-rt.jar"
    }
}

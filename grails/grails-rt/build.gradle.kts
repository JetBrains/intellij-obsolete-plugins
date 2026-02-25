// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
plugins {
    id("java")
}

group = "org.intellij.grails.rt"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.grails:grails-bootstrap:1.2.0")
}

java.sourceSets["main"].java {
    srcDir("src")
}
java.sourceSets["main"].resources {
    srcDir("resources")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }

}

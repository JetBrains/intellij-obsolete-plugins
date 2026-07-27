// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform.module")
}

group = "org.intellij.grails.maven"

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("org.jetbrains.idea.maven")
        // Java debugger APIs (GenericDebuggerRunner, DebuggerUtils, DebuggerSettings) are split
        // across these java-plugin modules in 2026.2
        bundledModule("intellij.java.debugger")
        bundledModule("intellij.java.debugger.impl")
        bundledModule("intellij.java.debugger.impl.shared")
    }

    compileOnly(project(":grails-rt"))
    compileOnly(project(":"))
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["main"].resources {
    srcDir("resources")
}

tasks {
        withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

}

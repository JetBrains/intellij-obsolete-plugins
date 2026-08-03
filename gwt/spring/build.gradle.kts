// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.tasks.ComposedJarTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

group = "com.intellij.gwt.spring"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("com.intellij.spring.mvc")
    }

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

    // The platform resolves a content module's isolated jar by the exact path
    // lib/modules/<content-module-name>.jar (PluginDescriptorLoader.loadPluginSubDescriptors).
    // The default composed-jar name would be "GWT.spring.jar", which does not match the
    // <module name="intellij.gwt.spring"/> declared in the root plugin.xml <content> block,
    // so the module would fail to load (fallback to the main classpath cannot find its
    // descriptor). Name the jar after the module so it loads with its own classloader.
    named<ComposedJarTask>("composedJar") {
        archiveBaseName = "intellij.gwt.spring"
    }
}

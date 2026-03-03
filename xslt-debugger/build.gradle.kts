import org.jetbrains.intellij.platform.gradle.Constants

// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "org.intellij.xsltDebugger"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

val xsltRuntime by configurations.creating

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("XPathView")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")

    implementation(project(":rt"))

    // plugin distribution runtime-only dependencies, copied to /lib/rt/
    xsltRuntime(project("rt:impl")) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                project.objects.named(Constants.Configurations.Attributes.COMPOSED_JAR_NAME))
        }
    }
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["main"].resources {
    srcDir("resources")
}

java.sourceSets["test"].java {
    srcDir("test")
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
        }

        changeNotes = ""
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

    prepareSandbox {
        // add 'rmi-stubs.jar'
        from(layout.projectDirectory.dir("rt/lib")) {
            into(pluginName.map { "$it/lib" })
        }

        // add xsltRuntime() dependencies
        from(configurations["xsltRuntime"]) {
            into(pluginName.map { "$it/lib/rt" })
        }
    }
}

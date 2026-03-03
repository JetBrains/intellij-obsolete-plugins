// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

group = "org.intellij.xsltDebugger.rt.impl"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        // we need to access runtime-only JAR from XPathView plugin in /lib/rt/
        // 'bundledPlugin' doesn't work here
        bundledLibrary("plugins/xpath/lib/rt/xslt-rt.jar")
    }

    implementation("xalan:xalan:2.7.3")
    implementation("xalan:serializer:2.7.3")
    implementation(files("lib/saxon.jar"))
    implementation(files("lib/saxon9he.jar"))

    compileOnly(project(":rt"))
    testImplementation(project(":rt"))

    testImplementation("junit:junit:4.13.2")
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["test"].java {
    srcDir("test")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

}

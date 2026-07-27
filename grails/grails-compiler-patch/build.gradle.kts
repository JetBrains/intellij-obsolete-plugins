// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

group = "org.intellij.grails.compiler.patch"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("org.intellij.groovy")
    }

    // grails-core 1.2.0 transitively drags in the groovy-all 1.6.7. Under 2026.2 that
    // shadows the modern Groovy compiler AST API this module is written against (e.g.
    // ModuleNode.addStaticStarImport). Exclude it and compile against a modern Groovy so the AST
    // types resolve; at JPS runtime the Grails project's own Groovy is used, so the compile-time
    // Groovy only needs matching fully-qualified names.
    compileOnly("org.grails:grails-core:1.2.0") {
        exclude(group = "org.codehaus.groovy", module = "groovy-all")
    }
    compileOnly("org.codehaus.groovy:groovy:3.0.25")
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

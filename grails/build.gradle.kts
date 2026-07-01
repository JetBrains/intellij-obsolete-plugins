// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "org.intellij.grails"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))

        bundledPlugin("com.intellij.javaee")
        bundledPlugin("com.intellij.persistence")
        bundledPlugin("com.intellij.javaee.jpa")
        bundledPlugin("com.intellij.jsp")
        bundledPlugin("com.intellij.javaee.web")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.database")
        bundledPlugin("com.intellij.spring")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("com.intellij.modules.ultimate")
        bundledPlugin("com.intellij.microservices.jvm")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.css")


        // plugin modules
        pluginModule(project(":copyright"))
        pluginModule(project(":coverage"))
        pluginModule(project(":hibernate"))
        pluginModule(project(":i18n"))
        pluginModule(project(":langInjection"))
        pluginModule(project(":maven"))

        // gradle-tooling classes must be visible to the main plugin classloader because
        // GrailsProjectResolverExtension references GrailsModule directly. Merging the
        // module into the main plugin jar via pluginComposedModule keeps it on the
        // main classloader instead of routing it through lib/modules/ (separate CL).
        pluginComposedModule(project(":gradle-tooling"))

        // additional plugins required for testing
        testBundledPlugin("com.intellij.hibernate")
        testBundledPlugin("HtmlTools")
        testBundledPlugin("org.jetbrains.idea.maven")

        testFramework(TestFrameworkType.Plugin.ExternalSystem)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
    testImplementation(project(":testFramework"))
    testCompileOnly(project(":i18n"))

    // compileOnly — implementation would auto-promote a pure-module project into
    // INTELLIJ_PLATFORM_PLUGIN_MODULE (lib/modules/). compileOnly is not scanned
    // for auto-promotion, so the merge via pluginComposedModule(...) above is what
    // ships gradle-tooling into the runtime distribution.
    compileOnly(project(":gradle-tooling"))
    implementation(project(":grails-rt"))

    // JPS/build
    runtimeOnly(project(":grails-compiler-patch"))
    runtimeOnly(project(":jps-plugin"))
}

kotlin {
    jvmToolchain(21)
}

java.sourceSets["main"].java {
    srcDir("src")
    srcDir("gen")
}

java.sourceSets["main"].resources {
    srcDir("resources")
    srcDir("compatibilityResources")
}

// special handling for /standardDsls/ packaging, see PrepareSandboxTask below
sourceSets {
    main {
        resources {
            exclude("standardDsls/")
        }
    }
}

java.sourceSets["test"].java {
    srcDir("test")
}

intellijPlatform {
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

    test {
        systemProperty("idea.home.path", properties("test.idea.home.path"))
    }

    // special handling for /standardDsls/ packaging
    withType<PrepareSandboxTask> {
        from(layout.projectDirectory.dir("resources/standardDsls")) {
            into(pluginName.map { "$it/lib/standardDsls/"})
        }
    }
}

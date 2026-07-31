// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.intellij.gwt"
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

        // hard dependencies (see resources/META-INF/plugin.xml <dependencies>)
        bundledPlugin("com.intellij.modules.ultimate")
        bundledPlugin("com.intellij.modules.idea.ultimate")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.css")
        bundledPlugin("com.intellij.java-i18n")
        bundledPlugin("com.intellij.javaee.app.servers.integration")
        bundledPlugin("com.intellij.javaee.web")

        // optional dependencies
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("JavaScriptDebugger")

        // plugin content modules
        pluginModule(project(":spring"))
        pluginModule(project(":dev"))

        // additional plugins required for testing
        testBundledPlugin("com.intellij.spring")
        testBundledPlugin("com.intellij.spring.mvc")
        testBundledPlugin("com.intellij.javaee")
        testBundledPlugin("org.jetbrains.idea.maven")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
        testFramework(TestFrameworkType.Plugin.ExternalSystem)
        testFramework(TestFrameworkType.JUnit5)
    }

    // protobuf-lite runtime for the generated RemoteMessageProto sources
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")

    // main sources reference org.jetbrains.jps.gwt.* classes at compile time;
    // runtimeOnly ships gwt-jps into the distribution for the external build process.
    compileOnly(project(":jps-plugin"))
    runtimeOnly(project(":jps-plugin"))
    implementation(project(":runtime"))

    testImplementation("junit:junit:4.13.2")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

java.sourceSets["main"].java {
    srcDir("src")
    srcDir("remoteUi/generated-source")
}

java.sourceSets["main"].resources {
    srcDir("resources")
}

// Active test sources. Tests that depend on JetBrains test frameworks not distributed with the
// released IDE (JavaEE-web, JavaScript, Spring, Grazie, artifact-compiler test bases) live under
// test-quarantined/ and are intentionally excluded from compilation. See test-quarantined/README.md.
java.sourceSets["test"].java {
    srcDir("test")
}

intellijPlatform {
    // GUI Designer form code is inlined in the dialog constructors (bound fields are final),
    // so bytecode form instrumentation is neither needed nor compatible.
    instrumentCode = false

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
        // GWT tests resolve fixtures relative to this directory (see GwtTestCase.getGwtTestDataPath)
        systemProperty("gwt.test.data.path", layout.projectDirectory.dir("testData").asFile.absolutePath)
    }

    // bundle the legacy SuperDevMode launcher jar into the plugin's lib/ directory
    withType<PrepareSandboxTask> {
        from(layout.projectDirectory.dir("lib")) {
            into(pluginName.map { "$it/lib/" })
            include("*.jar")
        }
    }
}

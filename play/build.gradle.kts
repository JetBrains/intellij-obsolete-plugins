plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.intellij"
version = "2025.3.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2025.3.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf("org.intellij.groovy", "com.intellij.persistence"))
}

// Configure Java toolchain to auto-download Java 21 if not available
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Enable toolchain auto-provisioning
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.release.set(17)
    }

    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }

    // Skip buildSearchableOptions if it fails (optional task)
    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

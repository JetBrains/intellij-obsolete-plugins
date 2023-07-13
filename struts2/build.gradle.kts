plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.intellij"
version = "2023.2.2"

repositories {
    mavenCentral()
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

intellij {
    version.set("2023.1.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf(
        "com.intellij.javaee",
        "com.intellij.javaee.web",
        "com.intellij.spring",
        "com.intellij.freemarker",
        "com.intellij.velocity",
        "org.intellij.groovy",
        "JavaScript",
        "com.intellij.java-i18n"
    ))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("232.*")
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

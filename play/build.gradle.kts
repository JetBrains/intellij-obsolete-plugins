plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.intellij"
version = "2023.2.1"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2.1")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf("org.intellij.groovy", "com.intellij.persistence"))
}

java.sourceSets["main"].java {
    srcDir("src/main/gen")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("241.*")
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

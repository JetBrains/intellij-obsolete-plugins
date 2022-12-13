plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.9.0"
}

group = "com.intellij"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("223-EAP-SNAPSHOT")
    type.set("IU")

    plugins.set(listOf(
            "com.intellij.java",
            "com.intellij.javaee",
            "com.intellij.javaee.app.servers.integration",
            "com.intellij.javaee.web",
            "com.intellij.spring",
            "Osmorc:223.7255.1"
    ))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
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

dependencies {
    implementation(fileTree("dir" to "lib", "include" to listOf("*.jar")))
}

java.sourceSets["main"].java {
    srcDir("dmServer-jps-plugin/src")
}
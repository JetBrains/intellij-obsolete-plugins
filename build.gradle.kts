fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.3.0"
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }
}

intellij {
    version.set("2020.3")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.intellij")

    version = "203." + (System.getenv("BUILD_NUMBER").takeIf { !it.isNullOrEmpty() } ?: "0")

    intellij {
        version.set("2020.3")
    }

    tasks {
        withType<JavaCompile> {
            sourceCompatibility = "11"
            targetCompatibility = "11"
        }

        patchPluginXml {
            sinceBuild.set("203.1")
            untilBuild.set("")
        }

        buildSearchableOptions {
            enabled = false
        }
    }
}

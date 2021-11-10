import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IC")
    plugins.set(listOf("java"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }

    patchPluginXml {
        sinceBuild.set("193.1")
        untilBuild.set("")
    }
}

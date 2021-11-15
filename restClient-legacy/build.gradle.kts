fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
}

val restClientIdeaVersion = properties("restClientIdeaVersion")

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IU")
    version.set(restClientIdeaVersion)
    plugins.set(listOf("com.jetbrains.restClient", "com.jetbrains.php:$restClientIdeaVersion"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("211.1")
        untilBuild.set("")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

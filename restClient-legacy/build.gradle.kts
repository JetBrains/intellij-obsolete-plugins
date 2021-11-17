fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
}

version = "213.0"

val restClientIdeaVersion = properties("restClientIdeaVersion")
val restClientPHPVersion = properties("restClientPHPVersion")

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IU")
    version.set(restClientIdeaVersion)
    plugins.set(listOf("com.jetbrains.restClient", "com.jetbrains.php:$restClientPHPVersion"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("")
        changeNotes.set("Make plugin compatible with 213. PHP Debug temporary unavailable because of platform changes.")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

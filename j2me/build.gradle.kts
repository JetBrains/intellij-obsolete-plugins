plugins {
    id ("java")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set( "IC")
    plugins.set( listOf("java", "java-i18n", "properties"))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

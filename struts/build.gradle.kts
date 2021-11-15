plugins {
    id("java")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    type.set("IU")
    version.set("2019.3")
    plugins.set(listOf(
        "java",
        "properties",
        "java-i18n",
        "jsp",
        "JavaEE",
        "JavaScriptLanguage",
        "CSS",
        "PersistenceSupport",
        "DatabaseTools",
        "struts2",
        "Spring",
        "SpringMvc",
    ))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

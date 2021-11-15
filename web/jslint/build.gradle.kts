plugins {
    id("java")
}

dependencies {
    implementation("org.jetbrains:annotations:17.0.0")
}

intellij {
    pluginName.set("JSLint")
    type.set("IU")
    plugins.set(listOf("JavaScriptLanguage"))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

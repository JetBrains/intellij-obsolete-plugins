intellij {
    type.set("IU")
    version.set("2021.2")
    plugins.set(listOf("java", "devkit", "android", "JavaEE", "com.intellij.gwt:212.4746.52", "javaFX", "ant", "Groovy"))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

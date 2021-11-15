intellij {
    type.set("IU")
    plugins.set(listOf("java", "devkit", "android", "JavaEE", "com.intellij.gwt:203.5981.155", "javaFX", "ant", "Groovy"))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

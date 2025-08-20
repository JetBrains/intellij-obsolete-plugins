intellij {
    type.set("IU")
    version.set("2022.3")
    plugins.set(listOf("java", "devkit", "android", "JavaEE", "com.intellij.gwt:223.7571.182", "javaFX", "ant", "Groovy"))
}

tasks {
    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

version = "223." + (System.getenv("BUILD_NUMBER").takeIf { !it.isNullOrEmpty() } ?: "0")

plugins {
    id("java")
}

version = "201.8743.12"

dependencies {
    testImplementation(files("../lib/ruby-test-framework-192.7056.jar"))
}

intellij {
    type.set("IU")
    version.set("201.8743.12")
    plugins.set(listOf("org.jetbrains.plugins.ruby:201.8743.12", "com.intellij.plugins.watcher:201.8538.6", "CSS", "sass"))
}


plugins {
    id("java")
}

sourceSets {
    main {
        java.srcDirs project . files ("src")
        resources.srcDirs project . files ("resources")
    }
    test {
        java.srcDirs project . files ("test")
    }
}

dependencies {
    testCompile files ("../lib/ruby-test-framework-192.7056.jar")
}

intellij {
    type.set("IU")
    plugins.set(listOf("java", "org.jetbrains.plugins.ruby:2019.2.20190917", "yaml"))
}

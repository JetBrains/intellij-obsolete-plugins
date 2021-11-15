dependencies {
    implementation(files("lib/jmock-1.0.1.jar"))
}

intellij {
    version.set("2021.1")
    plugins.set(listOf("java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    sourceSets {
        main {
            java.srcDirs(listOf("core/src", "src/idea", "src/jabber_idea", "src/jabber", "src/p2p"))
            resources.srcDirs(listOf("core/resources", "src/idea", "src/jabber/src"))
        }

        test {
            java.srcDirs("tests")
        }
    }

    patchPluginXml {
        sinceBuild.set("211.1")
        untilBuild.set("")
    }
}

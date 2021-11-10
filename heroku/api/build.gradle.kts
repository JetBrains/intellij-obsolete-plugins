plugins {
    id("java")
}

intellij {
    type.set("IU")
}

tasks {
    sourceSets {
        main {
            java.srcDirs(project.files("src"))
        }
    }
}

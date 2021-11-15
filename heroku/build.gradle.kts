plugins {
    id("java")
}

sourceSets {
    main {
        java.srcDirs(project.files("src"))
        resources.srcDirs(project.files("resources"))
    }
}

dependencies {
    implementation( project (":heroku:api"))
    implementation(project(":heroku:impl"))
}

intellij {
    type.set("IU")
    plugins.set(listOf("java", "JavaEE", "remote-run", "git4idea"))
}

sourceSets {
    main {
        java.srcDirs(listOf("cvs-core/src", "cvs-plugin/src", "javacvs-src", "smartcvs-src"))
        resources.srcDirs( listOf("cvs-core/resources", "cvs-plugin/resources", "javacvs-src", "smartcvs-src"))
    }

    test {
        java.srcDirs("testSource")
    }
}

dependencies {
    implementation(files("lib/trilead-ssh2-build213.jar"))
}

version = "223.0"

intellij {
    version.set("2022.3")
}

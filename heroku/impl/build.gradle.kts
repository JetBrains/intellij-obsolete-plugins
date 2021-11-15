plugins {
    id("java")
}

intellij {
    type.set("IU")
}

dependencies {
    implementation(project(":heroku:api"))
    implementation("commons-codec:commons-codec:1.9")
    implementation("commons-logging:commons-logging:1.2")
    implementation("org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b05")
    implementation("com.heroku.api:heroku-api:0.24")
    implementation("com.heroku.sdk:heroku-deploy:2.0.0-RC2") // was a snapshot
    implementation("com.heroku.api:heroku-http-jersey-client:0.24")
    implementation("org.glassfish.hk2:hk2-api:2.5.0-b05")
    implementation("org.glassfish.hk2:hk2-locator:2.5.0-b05")
    implementation("org.glassfish.hk2:hk2-utils:2.5.0-b05")
    implementation("org.apache.httpcomponents:httpclient:4.5")
    implementation("org.apache.httpcomponents:httpcore:4.4.1")
    implementation("org.javassist:javassist:3.20.0-GA")
    implementation("javax.annotation:javax.annotation-api:1.2")
    implementation("org.glassfish.hk2.external:javax.inject:2.5.0-b05")
    implementation("javax.ws.rs:javax.ws.rs-api:2.0.1")
    implementation("org.glassfish.jersey.connectors:jersey-apache-connector:2.24")
    implementation("org.glassfish.jersey.core:jersey-client:2.24")
    implementation("org.glassfish.jersey.core:jersey-common:2.24")
    implementation("org.glassfish.hk2:osgi-resource-locator:1.0.1")
}

tasks {
    sourceSets {
        main {
            java.srcDirs(project.files("src"))
        }
    }
}

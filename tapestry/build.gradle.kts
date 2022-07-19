plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.7.0"
}

group = "com.intellij"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("223.1192-EAP-CANDIDATE-SNAPSHOT")
  type.set("IU") // Target IDE Platform

  plugins.set(listOf(
    "com.intellij.javaee",
    "com.intellij.css",
    "com.intellij.properties"
  ))
}

sourceSets.getByName("main") {
  java.srcDir("src/main/gen")
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }

  patchPluginXml {
    sinceBuild.set("221")
    untilBuild.set("223.*")
  }
}

dependencies {
  implementation("commons-chain:commons-chain:1.2")

  testImplementation("org.testng:testng:7.6.1")
  testImplementation("org.easymock:easymock:4.0.2")
  testImplementation("org.objenesis:objenesis:3.2")
  testImplementation("org.xmlunit:xmlunit-core:2.9.0")
  testImplementation("org.xmlunit:xmlunit-matchers:2.9.0")
}
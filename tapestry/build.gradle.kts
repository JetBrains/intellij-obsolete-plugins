plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.10.0"
}

group = "com.intellij"
version = "223.7571.175"

repositories {
  mavenCentral()
}

intellij {
  version.set("2022.3")
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
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }

  patchPluginXml {
    sinceBuild.set("223")
    untilBuild.set("233.*")
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
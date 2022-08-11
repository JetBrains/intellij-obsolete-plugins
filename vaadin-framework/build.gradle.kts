plugins {
  id("java")
  id("org.jetbrains.intellij") version "1.8.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

intellij {
  version.set("2022.2")
  type.set("IU")

  plugins.set(listOf("org.jetbrains.idea.maven", "com.intellij.gwt:222.3345.118", "com.intellij.javaee"))
}

tasks {
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }

  patchPluginXml {
    sinceBuild.set("222")
    untilBuild.set("223.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}

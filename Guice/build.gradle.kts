import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.3.0"
  id("org.jetbrains.intellij.platform") version "2.16.0"
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

group = "com.intellij.guice"
version = "253.0.0"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    intellijIdea("2026.1")
    bundledPlugin("com.intellij.java")
    bundledPlugin("org.jetbrains.kotlin")
    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Plugin.Java)
    testFramework(TestFrameworkType.JUnit5)
  }
  testImplementation("com.google.truth:truth:1.4.2")
}

java {
  sourceSets.getByName("main") {
    java {
      srcDir("gen")
      srcDir("src")
    }
    kotlin {
      srcDir("gen")
      srcDir("src")
    }
    resources {
      srcDir("resources")
    }
  }
  sourceSets.getByName("test") {
    java {
      srcDir("test")
    }
    kotlin {
      srcDir("test")
    }
  }

}

kotlin {
  jvmToolchain(25)
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = "253"
    }

    changeNotes = ""
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    options.release = 25
    sourceCompatibility = "25"
    targetCompatibility = "25"
  }
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_25)
    }
  }
}

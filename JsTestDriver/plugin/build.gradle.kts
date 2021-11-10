import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(files("../lib/jstestdriver-core/JsTestDriver-1.3.5-patched.jar"))
    implementation(project(":JsTestDriver:common"))
    implementation(project(":JsTestDriver:rt"))
}

intellij {
    pluginName.set("JsTestDriver")
    type.set("IU")
    plugins.set(listOf("JavaScriptLanguage", "yaml", "coverage", "JavaScriptDebugger", "CSS"))
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_PLUGIN_TOKEN"))
    }
}

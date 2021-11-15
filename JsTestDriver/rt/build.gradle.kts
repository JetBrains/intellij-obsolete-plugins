plugins {
    id("java")
}

dependencies {
    implementation("org.jetbrains:annotations:17.0.0")
    implementation("com.google.guava:guava:25.1-jre")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(project(":JsTestDriver:common"))
    implementation(files("../lib/jstestdriver-core/JsTestDriver-1.3.5-patched.jar"))
    implementation(files("../lib/jstestdriver-core/coverage-1.3.5.jar"))
}

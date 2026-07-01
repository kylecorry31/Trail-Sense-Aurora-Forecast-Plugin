plugins {
    alias(libs.plugins.android.application) apply false
}

tasks.register("clean") {
    delete(rootProject.layout.buildDirectory)
}

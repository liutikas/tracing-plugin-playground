plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
}

dependencies {
    api(gradleApi())
    api(libs.kotlinGradlePlugin)
    implementation("androidx.tracing:tracing-driver-wire:1.0.0-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("MySettingsPlugin") {
            id = "MySettingsPlugin"
            implementationClass = "my.plugins.MySettingsPlugin"
        }
    }
}
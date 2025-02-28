plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "net.liutikas.tracing"
version = "0.0.1"

dependencies {
    api(gradleApi())
    api(libs.kotlinGradlePlugin)
    implementation("androidx.tracing:tracing-driver-wire:1.0.0-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("MySettingsPlugin") {
            id = "net.liutikas.tracing"
            implementationClass = "my.plugins.MySettingsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
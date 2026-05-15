plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "net.liutikas.tracing"
version = "0.0.1"

dependencies {
    api(gradleApi())
    implementation("androidx.tracing:tracing:2.0.0-alpha07")
    implementation("androidx.tracing:tracing-wire:2.0.0-alpha07")
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
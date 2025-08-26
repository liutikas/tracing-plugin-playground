pluginManagement {
    includeBuild("my-plugins")

    val properties = java.util.Properties()
    properties.load(file("gradle.properties").inputStream())

    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://androidx.dev/snapshots/builds/${properties["snapshotBuildId"]}/artifacts/repository")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("net.liutikas.tracing")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "tracing-plugin-playground"
include("libA")
include("libB")
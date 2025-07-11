pluginManagement {
    includeBuild("my-plugins")
    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://androidx.dev/snapshots/builds/13768719/artifacts/repository")
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

pluginManagement {
    includeBuild("my-plugins")
}

plugins {
    id("MySettingsPlugin")
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "smallest-settings-plugin"
include("lib")


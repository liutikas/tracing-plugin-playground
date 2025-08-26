dependencyResolutionManagement {
    val properties = java.util.Properties()
    properties.load(file("../gradle.properties").inputStream())

    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://androidx.dev/snapshots/builds/${properties["snapshotBuildId"]}/artifacts/repository")
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "tracing-gradle-plugin"
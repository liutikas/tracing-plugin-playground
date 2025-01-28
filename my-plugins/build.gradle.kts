plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-gradle-plugin")
}

dependencies {
    api(gradleApi())
    api(libs.kotlinGradlePlugin)
}

gradlePlugin {
    plugins {
        create("MySettingsPlugin") {
            id = "MySettingsPlugin"
            implementationClass = "my.plugins.MySettingsPlugin"
        }
    }
}
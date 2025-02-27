plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(libs.commons.math3)
    implementation(libs.guava)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.register<SlowTask>("a")

tasks.register<SlowTask>("b") {
    dependsOn("a")
}

tasks.register<SlowTask>("c") {
    dependsOn("a")
}

tasks.register<SlowTask>("d") {
    dependsOn("b")
}

tasks.register<SlowTask>("e") {
    dependsOn("b")
}

tasks.register<SlowTask>("f") {
    dependsOn("c")
}

abstract class SlowTask: DefaultTask() {
    @TaskAction
    fun beSlow() {
        Thread.sleep(500)
    }
}

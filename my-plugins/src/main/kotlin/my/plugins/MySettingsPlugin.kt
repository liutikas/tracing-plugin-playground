package my.plugins

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

class MySettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.gradle.beforeProject { project ->
            project.plugins.apply("org.jetbrains.kotlin.jvm")
            project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
                it.compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_1_9)
            }
        }
    }
}
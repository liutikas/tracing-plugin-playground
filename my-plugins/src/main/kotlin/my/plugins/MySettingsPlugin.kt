package my.plugins

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import javax.inject.Inject

@Suppress("unused")
abstract class MySettingsPlugin : Plugin<Settings> {
    @Suppress("UnstableApiUsage") // FlowScope
    @get:Inject
    abstract val flowScope: FlowScope

    override fun apply(settings: Settings) {

        @Suppress("UnstableApiUsage") // FlowScope
        flowScope.always(TracingServiceCloseAction::class.java) {}

        settings.gradle.beforeProject { project ->
            val tracingService =
                project.gradle.sharedServices.registerIfAbsent(
                    "tracingBuildService",
                    TracingBuildService::class.java
                ) { spec ->
                    spec.parameters.traceDir.set(
                        project.rootProject.isolated.projectDirectory.dir("trace").asFile
                    )
                }
            project.tasks.configureEach { task ->
                val deps = project.provider { task.taskDependencies.getDependencies(task).map { it.path } }
                task.doFirst {
                    val flowIds = (deps.get() + task.path)
                    tracingService.get().beginSection(task.path, flowIds)
                }
                task.doLast {
                    tracingService.get().endSection()
                }
            }
        }
    }
}

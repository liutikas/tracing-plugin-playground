package my.plugins

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import javax.inject.Inject
import java.io.File

@Suppress("unused")
abstract class MySettingsPlugin : Plugin<Settings> {
    @Suppress("UnstableApiUsage") // FlowScope
    @get:Inject
    abstract val flowScope: FlowScope

    override fun apply(settings: Settings) {
        @Suppress("UnstableApiUsage") // FlowScope
        flowScope.always(TracingServiceCloseAction::class.java) {}
        val tracingService =
            settings.gradle.sharedServices.registerIfAbsent(
                "tracingBuildService",
                TracingBuildService::class.java
            ) { spec ->
                spec.parameters.traceDir.set(File(settings.rootDir, "trace"))
            }
        tracingService.get().beginSection("settingsEvaluation")
        settings.gradle.settingsEvaluated {
            tracingService.get().endSection()
        }
        settings.gradle.projectsLoaded {
            tracingService.get().beginSection("configuration")
        }
        settings.gradle.beforeProject { project ->
            tracingService.get().beginSection(project.path)
            project.tasks.configureEach { task ->
                val deps = project.provider { task.taskDependencies.getDependencies(task).map { it.path } }
                task.doFirst {
                    val flowIds = (deps.get() + task.path)
                    tracingService.get().beginSection(task.path)
                }
                task.doLast {
                    tracingService.get().endSection()
                }
            }
        }
        settings.gradle.afterProject {
            tracingService.get().endSection()
        }
    }
}

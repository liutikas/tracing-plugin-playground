package my.plugins

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import javax.inject.Inject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
abstract class MySettingsPlugin : Plugin<Settings> {
    @Suppress("UnstableApiUsage") // FlowScope
    @get:Inject
    abstract val flowScope: FlowScope

    override fun apply(settings: Settings) {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        val subdirectory = formatter.format(Date())

        @Suppress("UnstableApiUsage") // FlowScope
        flowScope.always(TracingServiceCloseAction::class.java) {}
        val tracingService =
            settings.gradle.sharedServices.registerIfAbsent(
                "tracingBuildService",
                TracingBuildService::class.java
            ) { spec ->
                spec.parameters.traceDir.set(File(settings.rootDir, "trace/$subdirectory"))
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

        settings.gradle.projectsEvaluated {
            tracingService.get().endSection()
            tracingService.get().driver?.context?.close()
            tracingService.get().driver = null
        }

        settings.gradle.afterProject {
            tracingService.get().endSection()
        }
    }
}

package my.plugins

import org.gradle.api.Plugin
import org.gradle.api.flow.FlowScope
import org.gradle.api.initialization.Settings
import java.io.File
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
                settings.gradle.sharedServices.registerIfAbsent(
                    "tracingBuildService",
                    TracingBuildService::class.java
                ) { spec ->
                    spec.parameters.traceDir.set(File(settings.rootDir, "trace"))
                }
            project.tasks.configureEach { task ->
                task.doFirst {
                    tracingService.get().beginSection(task.path)
                }
                task.doLast {
                    tracingService.get().endSection()
                }
            }
        }
    }
}

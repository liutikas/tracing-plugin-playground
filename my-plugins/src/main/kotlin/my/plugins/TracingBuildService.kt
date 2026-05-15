package my.plugins

import androidx.tracing.wire.TraceDriver
import androidx.tracing.wire.TraceSink
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference

abstract class TracingBuildService : BuildService<TracingBuildService.Parameters> {
    init {
        log("new tracing build service")
    }
    interface Parameters : BuildServiceParameters {
        val traceDir: DirectoryProperty
    }

    var driver: TraceDriver? = null

    private fun getOrCreateDriver(): TraceDriver {
        if (driver == null) {
            driver = newDriver()
        }
        return driver!!
    }

    private fun newDriver(): TraceDriver {
        val dir = parameters.traceDir.get().asFile
        log("initialize driver in ${dir.absolutePath}")
        dir.mkdirs()
        return TraceDriver(sink =
            TraceSink(sequenceId = 1, directory = dir)
        )
    }

    fun beginSection(sectionName: String) {
        val tracer = getOrCreateDriver().tracer
        log("beginSection($sectionName)")
        tracer.beginSection("main", sectionName, null, false, { })
    }

    fun endSection() {
        getOrCreateDriver().context.process.currentThreadTrack().endSection()
        log("endSection()")
    }
}

@Suppress("UnstableApiUsage") // FlowParameters
abstract class TracingServiceCloseActionParameters : FlowParameters {
    @get:ServiceReference("tracingBuildService")
    abstract val traceBuildService: Property<TracingBuildService>
}

@Suppress("UnstableApiUsage") // FlowAction
abstract class TracingServiceCloseAction : FlowAction<TracingServiceCloseActionParameters> {
    override fun execute(parameters: TracingServiceCloseActionParameters) {
        log("build finished")
        if (parameters.traceBuildService.isPresent) {
            log("build finished - closing")
            parameters.traceBuildService.get().driver?.flush()
            parameters.traceBuildService.get().driver = null
        }
    }
}

private fun log(text: String) {
    if (VERBOSE_LOG) println(text)
}

private const val VERBOSE_LOG = true
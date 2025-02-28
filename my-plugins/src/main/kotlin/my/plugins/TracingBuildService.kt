package my.plugins

import androidx.tracing.driver.ThreadTrack
import androidx.tracing.driver.TraceDriver
import androidx.tracing.driver.wire.WireTraceSink
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference

abstract class TracingBuildService : BuildService<TracingBuildService.Parameters> {
    interface Parameters : BuildServiceParameters {
        val traceDir: DirectoryProperty
        val driver: Property<TraceDriver>
    }

    private val id = AtomicLong(0L)

    /** Generates a monotonically increasing [Long] value. */
    private fun monotonicId(): Long {
        return id.incrementAndGet()
    }

    private val flowIdMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
    private val previousFlowIds: ConcurrentHashMap<String, List<Long>> = ConcurrentHashMap()

    val driver: TraceDriver by lazy {
        println("initialize driver")
        val dir = parameters.traceDir.get().asFile
        //dir.deleteRecursively()
        dir.mkdirs()
        TraceDriver(sink = WireTraceSink(sequenceId = 1, directory = dir), isEnabled = true)
    }

    fun beginSection(sectionName: String) {
        val processTrack = driver.ProcessTrack(id = 1, name = "Process")
        val threadTrack = processTrack.getOrCreateThreadTrack(
            id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
            name = Thread.currentThread().name
        )
        println("beginSection($sectionName) for ${Thread.currentThread().name}")
        threadTrack.beginSection(sectionName)
    }

    fun beginSection(sectionName: String, dependencies: List<String>) {
        flowIdMap[sectionName] = monotonicId()
        val processTrack = driver.ProcessTrack(id = 1, name = "Process")
        val threadTrack = processTrack.getOrCreateThreadTrack(
            id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
            name = Thread.currentThread().name
        )
        val flowIds = (dependencies.map {
            flowIdMap[it]!!
        } + dependencies.flatMap { previousFlowIds[it] ?: listOf() }).distinct().sorted()
        previousFlowIds[sectionName] = flowIds
        println("beginSection($sectionName, [${flowIds.joinToString(", ")}])")
        threadTrack.beginSection(sectionName, flowIds)
    }

    fun endSection() {
        val processTrack = driver.ProcessTrack(id = 1, name = "Process")
        val threadTrack =
            processTrack.getOrCreateThreadTrack(
                id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
                name = Thread.currentThread().name
            )
        println("endSection() for ${Thread.currentThread().name}")
        threadTrack.endSection()
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
        println("build finished")
        if (parameters.traceBuildService.isPresent) {
            parameters.traceBuildService.get().driver.context.close()
        }
    }
}

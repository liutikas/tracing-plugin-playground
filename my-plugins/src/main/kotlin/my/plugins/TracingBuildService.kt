package my.plugins

import androidx.tracing.driver.TraceDriver
import androidx.tracing.driver.wire.TraceSink
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.ServiceReference
import java.io.File

abstract class TracingBuildService : BuildService<TracingBuildService.Parameters> {
    init {
        log("new tracing build service")
    }
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

    var driver: TraceDriver? = null

    public fun getOrCreateDriver(): TraceDriver {
        if (driver == null) {
            driver = newDriver()
        }
        return driver!!
    }

    private fun newDriver(): TraceDriver {
        log("initialize driver")
        val dir = parameters.traceDir.get().asFile
        dir.mkdirs()
        return TraceDriver(sink =
            TraceSink(sequenceId = 1, directory = dir), isEnabled = true
        )
    }

    fun beginSection(sectionName: String) {
        val processTrack = getOrCreateDriver().ProcessTrack(id = 1, name = "Process")
        val threadTrack = processTrack.getOrCreateThreadTrack(
            id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
            name = Thread.currentThread().name
        )
        log("beginSection($sectionName) for ${Thread.currentThread().name}")
        threadTrack.beginSection(sectionName)
    }

    fun beginSection(sectionName: String, dependencies: List<String>) {
        flowIdMap[sectionName] = monotonicId()
        val processTrack = getOrCreateDriver().ProcessTrack(id = 1, name = "Process")
        val threadTrack = processTrack.getOrCreateThreadTrack(
            id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
            name = Thread.currentThread().name
        )
        val flowIds = (dependencies.map {
            flowIdMap[it]!!
        } + dependencies.flatMap { previousFlowIds[it] ?: listOf() }).distinct().sorted()
        previousFlowIds[sectionName] = flowIds
        log("beginSection($sectionName, [${flowIds.joinToString(", ")}])")
        threadTrack.beginSection(sectionName, flowIds)
    }

    fun endSection() {
        val processTrack = getOrCreateDriver().ProcessTrack(id = 1, name = "Process")
        val threadTrack =
            processTrack.getOrCreateThreadTrack(
                id = @Suppress("DEPRECATION") Thread.currentThread().id.toInt(),
                name = Thread.currentThread().name
            )
        log("endSection() for ${Thread.currentThread().name}")
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
        log("build finished")
        if (parameters.traceBuildService.isPresent) {
            log("build finished - closing")
            parameters.traceBuildService.get().driver?.context?.close()
            parameters.traceBuildService.get().driver = null

            val traceDir = parameters.traceBuildService.get().parameters.traceDir.get().asFile
            createZipFile(
                traceDir.listFiles() ?: emptyArray(),
                File(traceDir, "merged.zip")
            )
        }
    }
}

private fun createZipFile(files: Array<File>, outputZipFile: File): File {
    ZipOutputStream(FileOutputStream(outputZipFile)).use { zipOut ->
        files.forEach { file ->
            FileInputStream(file).use { fis ->
                val zipEntry = ZipEntry(file.name)
                zipOut.putNextEntry(zipEntry)
                fis.copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
    }
    return outputZipFile
}

private fun log(text: String) {
    if (VERBOSE_LOG) println(text)
}

private const val VERBOSE_LOG = false
package me.whereareiam.toolkit.distribution.task

import me.whereareiam.toolkit.distribution.extension.DistributionEntry
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Copies every declared file to `<name>-<label>-<version>.<extension>` and packs archive members
 * into `<name>-<archive>-<version>.zip`. The output directory holds nothing else afterwards.
 */
@DisableCachingByDefault(because = "Copying files is cheaper than caching them")
abstract class AssembleDistributionTask : DefaultTask() {

    @get:Input
    abstract val distributionName: Property<String>

    @get:Input
    abstract val distributionVersion: Property<String>

    @get:Nested
    abstract val entries: ListProperty<DistributionEntry>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun assemble() {
        val declared = entries.get()
        if (declared.isEmpty()) throw GradleException("toolkitDistribution declares no files")

        val files = declared.map { Resolved(fileName(it.label.get(), source(it)), it.archive.orNull, source(it)) }
        val archives = files.mapNotNull(Resolved::archive).distinct().map { fileName(it, "zip") to it }
        val topLevel = files.filter { it.archive == null }.map(Resolved::name) + archives.map { it.first }
        requireUnique(topLevel, "the distribution")

        val directory = outputDirectory.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()

        files.filter { it.archive == null }.forEach { it.source.copyTo(File(directory, it.name)) }
        for ((archiveName, label) in archives) {
            val members = files.filter { it.archive == label }.sortedBy(Resolved::name)
            requireUnique(members.map(Resolved::name), "archive $label")
            zip(File(directory, archiveName), members)
        }
    }

    private fun source(entry: DistributionEntry): File {
        val sources = entry.source.files
        if (sources.size != 1 || !sources.single().isFile)
            throw GradleException("Distribution file '${entry.label.get()}' must come from exactly one file, got $sources")
        return sources.single()
    }

    private fun fileName(label: String, source: File): String = fileName(label, source.extension)

    private fun fileName(label: String, extension: String): String {
        if (!LABEL.matches(label)) throw GradleException("Distribution label '$label' may only contain letters, digits, '.', '_' and '-'")
        return "${distributionName.get()}-$label-${distributionVersion.get()}.$extension"
    }

    private fun requireUnique(names: List<String>, owner: String) {
        val duplicates = names.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) throw GradleException("Duplicate files in $owner: $duplicates")
    }

    // Sorted members and a fixed timestamp keep the archive identical for identical inputs.
    private fun zip(target: File, members: List<Resolved>) {
        ZipOutputStream(target.outputStream().buffered()).use { output ->
            for (member in members) {
                output.putNextEntry(ZipEntry(member.name).apply { time = ARCHIVE_TIMESTAMP })
                member.source.inputStream().use { it.copyTo(output) }
                output.closeEntry()
            }
        }
    }

    private class Resolved(val name: String, val archive: String?, val source: File)

    private companion object {
        val LABEL = Regex("[A-Za-z0-9._-]+")

        // 1980-02-01, the constant Gradle's reproducible archives use.
        const val ARCHIVE_TIMESTAMP = 318_207_600_000L
    }
}

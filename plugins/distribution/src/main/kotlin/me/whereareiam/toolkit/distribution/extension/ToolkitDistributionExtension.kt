package me.whereareiam.toolkit.distribution.extension

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ToolkitDistributionExtension @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory
) {

    /** First part of every file name; defaults to the root project's name. */
    abstract val name: Property<String>

    /** Last part of every file name; defaults to the project's version. */
    abstract val version: Property<String>

    /** Directory the distribution is assembled into; defaults to `build/distribution`. */
    abstract val directory: DirectoryProperty

    /** Declared files in declaration order; filled through [file] and [archive]. */
    abstract val entries: ListProperty<DistributionEntry>

    private val files = DistributionFiles(project, objects, entries, null)

    /** Adds a top-level file; see [DistributionFiles.file]. */
    fun file(label: String, source: Any) = files.file(label, source)

    /** Adds a top-level file from a task of another project; see [DistributionFiles.file]. */
    fun file(label: String, projectPath: String, taskName: String) = files.file(label, projectPath, taskName)

    /**
     * Packs the files declared in the action into `<name>-<label>-<version>.zip`. They keep their
     * final names inside the archive and are not written beside it.
     */
    fun archive(label: String, action: Action<DistributionFiles>) =
        action.execute(DistributionFiles(project, objects, entries, label))
}

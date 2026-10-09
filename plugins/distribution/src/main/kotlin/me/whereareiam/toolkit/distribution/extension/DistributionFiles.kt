package me.whereareiam.toolkit.distribution.extension

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty

/**
 * Declares the files of a distribution, either at its top level or inside one archive.
 */
class DistributionFiles internal constructor(
    private val project: Project,
    private val objects: ObjectFactory,
    private val entries: ListProperty<DistributionEntry>,
    private val archive: String?
) {

    /**
     * Adds a file named `<name>-<label>-<version>.<extension>`. The source is anything a file
     * collection accepts, such as an archive task or its provider, and must resolve to one file.
     */
    fun file(label: String, source: Any) {
        val entry = objects.newInstance(DistributionEntry::class.java)
        entry.label.set(label)
        archive?.let(entry.archive::set)
        entry.source.from(source)
        entries.add(entry)
    }

    /**
     * Adds the single output of a task of another project. The task is looked up when the
     * distribution is assembled, so the project does not have to be evaluated yet.
     */
    fun file(label: String, projectPath: String, taskName: String) {
        val owner = project.project(projectPath)
        file(label, project.provider { owner.tasks.named(taskName).get().outputs.files })
    }
}

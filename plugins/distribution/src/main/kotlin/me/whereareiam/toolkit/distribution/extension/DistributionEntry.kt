package me.whereareiam.toolkit.distribution.extension

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * One file of a distribution: the label in its final name, the single file it is copied from,
 * and the archive it is packed into, if any.
 */
abstract class DistributionEntry {

    @get:Input
    abstract val label: Property<String>

    @get:Input
    @get:Optional
    abstract val archive: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val source: ConfigurableFileCollection
}

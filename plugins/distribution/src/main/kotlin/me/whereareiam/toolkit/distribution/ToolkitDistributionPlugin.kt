package me.whereareiam.toolkit.distribution

import me.whereareiam.toolkit.distribution.extension.ToolkitDistributionExtension
import me.whereareiam.toolkit.distribution.task.AssembleDistributionTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class ToolkitDistributionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("toolkitDistribution", ToolkitDistributionExtension::class.java)

        extension.name.convention(project.rootProject.name)
        extension.version.convention(project.provider { project.version.toString() })
        extension.directory.convention(project.layout.buildDirectory.dir("distribution"))

        project.tasks.register(TASK_NAME, AssembleDistributionTask::class.java) {
            group = "distribution"
            description = "Collects the files a release ships under their final names."
            distributionName.set(extension.name)
            distributionVersion.set(extension.version)
            entries.set(extension.entries)
            outputDirectory.set(extension.directory)
        }
    }

    companion object {
        const val TASK_NAME = "assembleDistribution"
    }
}

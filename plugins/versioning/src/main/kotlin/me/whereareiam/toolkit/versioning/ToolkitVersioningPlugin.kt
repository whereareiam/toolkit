package me.whereareiam.toolkit.versioning

import me.whereareiam.toolkit.versioning.extension.ToolkitVersioningExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ToolkitVersioningPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("toolkitVersioning", ToolkitVersioningExtension::class.java)

        extension.defaultVersion.convention("dev")
        extension.environmentVariableName.convention("VERSION")
        extension.releasePattern.convention(ToolkitVersioningSupport.DEFAULT_RELEASE_PATTERN)

        project.afterEvaluate {
            project.version = extension.resolvedVersion().get()
        }
    }
}

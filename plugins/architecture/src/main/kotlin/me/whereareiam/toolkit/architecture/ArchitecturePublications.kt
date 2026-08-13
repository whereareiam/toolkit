package me.whereareiam.toolkit.architecture

import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import org.gradle.api.Project

/** Finds classified projects that publish Maven libraries. */
object ArchitecturePublications {
	fun publishedProjects(rootProject: Project): List<Project> = rootProject.allprojects
		.filter { project ->
			project.extensions.findByType(ArchitectureExtension::class.java) != null
				&& project.plugins.hasPlugin("maven-publish")
				&& !project.plugins.hasPlugin("java-gradle-plugin")
		}
		.sortedBy(Project::getPath)
}

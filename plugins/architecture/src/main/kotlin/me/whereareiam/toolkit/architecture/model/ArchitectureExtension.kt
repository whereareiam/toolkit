package me.whereareiam.toolkit.architecture.model

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/** Architecture classification inferred from project structure and conventions. */
abstract class ArchitectureExtension {
	var kind: ArchitectureKind? = null

	val api: ArchitectureKind
		get() = ArchitectureKind.API
	val implementation: ArchitectureKind
		get() = ArchitectureKind.IMPLEMENTATION
	val assembly: ArchitectureKind
		get() = ArchitectureKind.ASSEMBLY

	internal fun descriptor(project: Project): ArchitectureDescriptor {
		val projectKind = kind ?: inferKind(project)
		return ArchitectureDescriptor(projectKind, isRootApi(project, projectKind))
	}

	private fun inferKind(project: Project): ArchitectureKind {
		if (project.name.endsWith("-api"))
			return ArchitectureKind.API

		if (project.pluginManager.hasPlugin("java-gradle-plugin"))
			return ArchitectureKind.ASSEMBLY

		if (project.path.endsWith(":default"))
			return ArchitectureKind.ASSEMBLY

		if (isSourceFree(project) && hasProjectDependencies(project))
			return ArchitectureKind.ASSEMBLY

		return ArchitectureKind.IMPLEMENTATION
	}

	private fun isRootApi(project: Project, projectKind: ArchitectureKind): Boolean =
		projectKind == ArchitectureKind.API && project.parent == project.rootProject

	private fun isSourceFree(project: Project): Boolean = !project.file("src/main").exists()

	private fun hasProjectDependencies(project: Project): Boolean = project.configurations
		.filter { it.name in productionConfigurations }
		.any { configuration -> configuration.dependencies.any { it is ProjectDependency } }

	private companion object {
		val productionConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")
	}
}

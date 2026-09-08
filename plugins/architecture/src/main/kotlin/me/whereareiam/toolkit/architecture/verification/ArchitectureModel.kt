package me.whereareiam.toolkit.architecture.verification

import me.whereareiam.toolkit.architecture.model.ArchitectureDescriptor
import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/** Infers functional API owners without treating organizational ancestors as one family. */
internal class ArchitectureModel(private val root: Project) {
	private val projects = root.allprojects.sortedBy(Project::getPath)
	private val extensions = projects.associateWith { it.extensions.findByType(ArchitectureExtension::class.java) }
	private val kinds = projects.associateWith { extensions[it]?.kind ?: inferKind(it) }
	private val shared = projects.associateWith { project ->
		extensions[project]?.rootApi ?: (kinds[project] == ArchitectureKind.API
				&& project.parent == root
				&& (project.name == "api" || project.name.equals("${root.name}-api", ignoreCase = true)))
	}

	val descriptors: Map<String, ArchitectureDescriptor> = projects.associate { project ->
		project.path to ArchitectureDescriptor(kinds.getValue(project), shared.getValue(project), family(project))
	}

	fun configurationViolations(): List<String> = buildList {
		for (project in projects) {
			if (extensions[project]?.family?.isBlank() == true)
				add("${project.path}: architecture family must not be blank")
			if (shared.getValue(project) && kinds[project] != ArchitectureKind.API)
				add("${project.path}: shared root API must have API kind")
		}

		val roots = projects.filter { shared.getValue(it) && kinds[it] == ArchitectureKind.API }
		if (roots.size > 1)
			add("Only one shared root API is allowed; found ${roots.joinToString { it.path }}")
	}

	private fun family(project: Project): String {
		extensions[project]?.family?.takeIf(String::isNotBlank)?.let { return it }
		if (shared.getValue(project)) return project.path

		if (kinds[project] == ArchitectureKind.API) {
			val owner = project.parent
			if (owner == null || owner == root) return project.path
			return extensions[owner]?.family?.takeIf(String::isNotBlank) ?: owner.path
		}

		var owner: Project? = project
		while (owner != null && owner != root) {
			if (owner.childProjects.values.any { kinds[it] == ArchitectureKind.API && shared[it] != true })
				return extensions[owner]?.family?.takeIf(String::isNotBlank) ?: owner.path
			owner = owner.parent
		}

		return project.path
	}

	private fun isSourceFree(project: Project): Boolean {
		val conventionalRoot = project.file("src/main").toPath().toAbsolutePath().normalize()
		if (conventionalRoot.toFile().exists()) return false
		val main = project.extensions.findByType(SourceSetContainer::class.java)
				?.findByName(SourceSet.MAIN_SOURCE_SET_NAME) ?: return !project.file("src").exists()
		if (main.allSource.buildDependencies.getDependencies(null).isNotEmpty()) return false

		return main.allSource.srcDirs.all { it.toPath().toAbsolutePath().normalize().startsWith(conventionalRoot) }
	}

	private fun inferKind(project: Project): ArchitectureKind {
		if (project.name == "api" || project.name.endsWith("-api")) return ArchitectureKind.API
		if (project.pluginManager.hasPlugin("java-gradle-plugin")) return ArchitectureKind.ASSEMBLY
		if (isSourceFree(project) && ProductionConfigurations.of(project)
				.any { it.dependencies.any { dependency -> dependency is ProjectDependency } })
			return ArchitectureKind.ASSEMBLY

		return ArchitectureKind.IMPLEMENTATION
	}
}

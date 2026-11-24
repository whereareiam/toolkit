package me.whereareiam.toolkit.architecture

import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.architecture.verification.ProductionConfigurations
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * Classifies a project as verification does: by the kind its `architecture` block states, else by its name,
 * its plugins and its sources. Other plugins ask it for a project's role instead of guessing from the name.
 */
object ArchitectureKinds {
	/**
	 * The kind of [project]: the `kind` its `architecture` block states where the plugin is applied to it,
	 * otherwise the inferred one. `api` and names ending in `-api` are APIs, Gradle-plugin projects and
	 * source-free dependency bundles are assemblies, and everything else is an implementation.
	 *
	 * @throws GradleException when the architecture plugin is applied to [project] from another class loader,
	 * so that its `architecture` block cannot be read here
	 */
	fun of(project: Project): ArchitectureKind {
		val extension = project.extensions.findByType(ArchitectureExtension::class.java)
		if (extension == null && project.pluginManager.hasPlugin(ToolkitArchitecturePlugin.ID))
			throw GradleException(
				"${project.path} applies ${ToolkitArchitecturePlugin.ID} from another class loader than the plugin that asks for its " +
					"architecture kind; load both plugins in the same build script classpath"
			)

		return extension?.kind ?: inferred(project)
	}

	private fun inferred(project: Project): ArchitectureKind {
		if (project.name == "api" || project.name.endsWith("-api")) return ArchitectureKind.API
		if (project.pluginManager.hasPlugin("java-gradle-plugin")) return ArchitectureKind.ASSEMBLY
		if (isSourceFree(project) && ProductionConfigurations.of(project)
				.any { it.dependencies.any { dependency -> dependency is ProjectDependency } })
			return ArchitectureKind.ASSEMBLY

		return ArchitectureKind.IMPLEMENTATION
	}

	private fun isSourceFree(project: Project): Boolean {
		val conventionalRoot = project.file("src/main").toPath().toAbsolutePath().normalize()
		if (conventionalRoot.toFile().exists()) return false
		val main = project.extensions.findByType(SourceSetContainer::class.java)
				?.findByName(SourceSet.MAIN_SOURCE_SET_NAME) ?: return !project.file("src").exists()
		if (main.allSource.buildDependencies.getDependencies(null).isNotEmpty()) return false

		return main.allSource.srcDirs.all { it.toPath().toAbsolutePath().normalize().startsWith(conventionalRoot) }
	}
}

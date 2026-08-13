package me.whereareiam.toolkit.architecture.verification

import me.whereareiam.toolkit.architecture.model.ArchitectureDescriptor
import me.whereareiam.toolkit.architecture.model.ArchitectureExtension
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

internal object ArchitectureVerifier {
	private val productionConfigurations = setOf("api", "implementation", "compileOnly", "runtimeOnly")

	fun violations(rootProject: Project): List<String> {
		val descriptors = descriptors(rootProject)
		return rootProject.allprojects.flatMap { projectViolations(it, descriptors) }.sorted()
	}

	private fun descriptors(rootProject: Project): Map<String, ArchitectureDescriptor> = rootProject.allprojects
		.mapNotNull { project ->
			project.extensions.findByType(ArchitectureExtension::class.java)
				?.descriptor(project)
				?.let { project.path to it }
		}
		.toMap()

	private fun projectViolations(
		source: Project,
		descriptors: Map<String, ArchitectureDescriptor>
	): List<String> {
		val sourceDescriptor = descriptors[source.path] ?: return emptyList()
		return productionConfigurations.flatMap { configurationName ->
			val configuration = source.configurations.findByName(configurationName) ?: return@flatMap emptyList()
			configuration.dependencies.withType(ProjectDependency::class.java).flatMap { dependency ->
				dependencyViolations(source, sourceDescriptor, configurationName, dependency, descriptors)
			}
		}
	}

	private fun dependencyViolations(
		source: Project,
		sourceDescriptor: ArchitectureDescriptor,
		configurationName: String,
		dependency: ProjectDependency,
		descriptors: Map<String, ArchitectureDescriptor>
	): List<String> {
		val targetDescriptor = descriptors[dependency.path]
			?: return listOf("${source.path}:$configurationName targets unclassified project ${dependency.path}")

		if (sourceDescriptor.rootApi)
			return listOf("${source.path} must not depend on ${dependency.path}")

		return when (sourceDescriptor.kind) {
			ArchitectureKind.API -> apiViolations(source, configurationName, dependency, targetDescriptor)
			ArchitectureKind.IMPLEMENTATION -> implementationViolations(source, configurationName, dependency, targetDescriptor)
			ArchitectureKind.ASSEMBLY -> emptyList()
		}
	}

	private fun apiViolations(
		source: Project,
		configurationName: String,
		dependency: ProjectDependency,
		targetDescriptor: ArchitectureDescriptor
	): List<String> {
		if (targetDescriptor.kind == ArchitectureKind.API && configurationName == "api")
			return emptyList()

		return buildList {
			if (targetDescriptor.kind != ArchitectureKind.API)
				add("${source.path}:$configurationName may depend only on APIs, not ${dependency.path} (${targetDescriptor.kind})")
			if (configurationName != "api")
				add("${source.path} must declare public API project dependencies with api, not $configurationName")
		}
	}

	private fun implementationViolations(
		source: Project,
		configurationName: String,
		dependency: ProjectDependency,
		targetDescriptor: ArchitectureDescriptor
	): List<String> {
		if (targetDescriptor.kind == ArchitectureKind.API)
			return emptyList()

		return listOf("${source.path}:$configurationName may depend only on APIs, not ${dependency.path} (${targetDescriptor.kind})")
	}
}

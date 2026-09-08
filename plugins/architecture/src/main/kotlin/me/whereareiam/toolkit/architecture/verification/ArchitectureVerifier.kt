package me.whereareiam.toolkit.architecture.verification

import me.whereareiam.toolkit.architecture.model.ArchitectureDescriptor
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

internal object ArchitectureVerifier {
	fun violations(rootProject: Project): List<String> {
		val model = ArchitectureModel(rootProject)
		return (model.configurationViolations() + rootProject.allprojects.flatMap { project ->
			projectViolations(project, model.descriptors)
		}).distinct().sorted()
	}

	private fun projectViolations(
		source: Project,
		descriptors: Map<String, ArchitectureDescriptor>
	): List<String> {
		val sourceDescriptor = descriptors.getValue(source.path)
		val exported = ProductionConfigurations.exportedByApi(source)
		return ProductionConfigurations.of(source).flatMap { configuration ->
			configuration.dependencies.withType(ProjectDependency::class.java).flatMap { dependency ->
				dependencyViolations(source, sourceDescriptor, configuration.name, configuration in exported,
						dependency, descriptors)
			}
		}
	}

	private fun dependencyViolations(
		source: Project,
		sourceDescriptor: ArchitectureDescriptor,
		configurationName: String,
		apiExport: Boolean,
		dependency: ProjectDependency,
		descriptors: Map<String, ArchitectureDescriptor>
	): List<String> {
		val targetDescriptor = descriptors[dependency.path]
			?: return listOf("${source.path}:$configurationName targets unclassified project ${dependency.path}")

		if (sourceDescriptor.rootApi)
			return listOf("${source.path}:$configurationName must not depend on ${dependency.path} (shared root API)")
		if (sourceDescriptor.kind == ArchitectureKind.ASSEMBLY) return emptyList()

		val reference = "${source.path}:$configurationName -> ${dependency.path}"
		return buildList {
			if (targetDescriptor.kind != ArchitectureKind.API) {
				add("$reference may depend only on APIs, not ${targetDescriptor.kind}")
			} else if (!targetDescriptor.rootApi && targetDescriptor.family != sourceDescriptor.family) {
				add("$reference crosses API family '${sourceDescriptor.family}' -> '${targetDescriptor.family}'; "
						+ "only own-family APIs and the shared root API are allowed")
			}

			if (sourceDescriptor.kind == ArchitectureKind.API && !apiExport)
				add("$reference must expose API project dependencies through api, not $configurationName")
		}
	}
}

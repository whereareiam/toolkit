package me.whereareiam.toolkit.architecture.verification

import me.whereareiam.toolkit.architecture.model.ArchitectureDescriptor
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

internal object ArchitectureVerifier {
	fun violations(rootProject: Project): List<String> {
		val model = ArchitectureModel(rootProject)
		return (model.configurationViolations() + rootProject.allprojects.flatMap { project ->
			projectViolations(project, model)
		}).distinct().sorted()
	}

	private fun projectViolations(
		source: Project,
		model: ArchitectureModel
	): List<String> {
		val sourceDescriptor = model.descriptors.getValue(source.path)
		val exported = ProductionConfigurations.exportedByApi(source)
		return ProductionConfigurations.of(source).flatMap { configuration ->
			configuration.dependencies.withType(ProjectDependency::class.java).flatMap { dependency ->
				dependencyViolations(source, sourceDescriptor, configuration.name, configuration in exported,
						dependency, model)
			}
		}
	}

	private fun dependencyViolations(
		source: Project,
		sourceDescriptor: ArchitectureDescriptor,
		configurationName: String,
		apiExport: Boolean,
		dependency: ProjectDependency,
		model: ArchitectureModel
	): List<String> {
		val targetDescriptor = model.descriptors[dependency.path]
			?: return listOf("${source.path}:$configurationName targets unclassified project ${dependency.path}")

		if (sourceDescriptor.rootApi)
			return listOf("${source.path}:$configurationName must not depend on ${dependency.path} (shared root API)")
		if (sourceDescriptor.kind == ArchitectureKind.ASSEMBLY) return emptyList()

		val reference = "${source.path}:$configurationName -> ${dependency.path}"
		return buildList {
			if (targetDescriptor.kind != ArchitectureKind.API) {
				add("$reference may depend only on APIs, not ${targetDescriptor.kind}")
			} else if (!allows(sourceDescriptor, dependency.path, targetDescriptor)) {
				add("$reference crosses API family '${sourceDescriptor.family}' -> '${targetDescriptor.family}'; "
						+ "only own-family APIs, explicitly shared APIs, and the shared root API are allowed")
			} else {
				addAll(exportViolations(reference, dependency.path, sourceDescriptor, model))
			}

			if (sourceDescriptor.kind == ArchitectureKind.API && !apiExport)
				add("$reference must expose API project dependencies through api, not $configurationName")
		}
	}

	private fun allows(source: ArchitectureDescriptor, targetPath: String, target: ArchitectureDescriptor): Boolean =
		target.kind == ArchitectureKind.API && (target.rootApi || target.family == source.family || targetPath in source.sharedApis)

	private fun exportViolations(
		reference: String,
		apiPath: String,
		source: ArchitectureDescriptor,
		model: ArchitectureModel
	): List<String> {
		val visited = mutableSetOf(apiPath)
		val violations = mutableListOf<String>()
		fun visit(path: String) {
			for (exported in model.apiExports[path].orEmpty()) {
				if (!visited.add(exported)) continue
				val target = model.descriptors[exported] ?: continue
				if (!allows(source, exported, target))
					violations.add("$reference exposes unapproved API $exported through $path; sharedApis permissions are not transitive")
				visit(exported)
			}
		}
		visit(apiPath)

		return violations
	}
}

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
	private val owners = projects.associateWith(::owner)

	val descriptors: Map<String, ArchitectureDescriptor> = projects.associate { project ->
		project.path to ArchitectureDescriptor(
			kinds.getValue(project), shared.getValue(project), family(project), sharedApis(project)
		)
	}
	val apiExports: Map<String, List<String>> = projects
		.filter { kinds[it] == ArchitectureKind.API }
		.associate { project ->
			project.path to ProductionConfigurations.exportedByApi(project)
				.flatMap { it.dependencies.withType(ProjectDependency::class.java) }
				.map(ProjectDependency::getPath).distinct().sorted()
		}

	fun configurationViolations(): List<String> = buildList {
		for (project in projects) {
			if (extensions[project]?.family?.isBlank() == true)
				add("${project.path}: architecture family must not be blank")
			if (shared.getValue(project) && kinds[project] != ArchitectureKind.API)
				add("${project.path}: shared root API must have API kind")
			addAll(sharedApiViolations(project))
		}

		val roots = projects.filter { shared.getValue(it) && kinds[it] == ArchitectureKind.API }
		if (roots.size > 1)
			add("Only one shared root API is allowed; found ${roots.joinToString { it.path }}")
		addAll(sharedApiCycles())
	}

	private fun sharedApis(project: Project): Set<String> {
		val own = extensions[project]?.sharedApis.orEmpty()
		val owner = owners.getValue(project)
		if (owner == project || family(project) != family(owner)) return own

		return own + extensions[owner]?.sharedApis.orEmpty()
	}

	private fun sharedApiViolations(project: Project): List<String> = buildList {
		val declared = extensions[project]?.sharedApis.orEmpty()
		if (declared.isEmpty()) return@buildList
		val source = descriptors.getValue(project.path)
		if (source.rootApi)
			add("${project.path}: shared root API must not declare sharedApis")
		if (source.kind != ArchitectureKind.API && !ownsApi(project))
			add("${project.path}: sharedApis must be declared on an API or its owning project")

		for (path in declared.sorted()) {
			val target = descriptors[path]
			when {
				target == null -> add("${project.path}: sharedApis targets unknown API project '$path'; use an absolute project path")
				target.kind != ArchitectureKind.API -> add("${project.path}: sharedApis target $path must have API kind")
				target.rootApi -> add("${project.path}: sharedApis must not include the shared root API $path; it is already allowed")
				target.family == source.family -> add("${project.path}: sharedApis must not include its own API family: $path")
				!isAncestorContract(project, path) -> add("${project.path}: sharedApis target $path must belong to a strict ancestor of its API owner; sibling and unrelated API families cannot be shared")
			}
		}
	}

	private fun isAncestorContract(project: Project, targetPath: String): Boolean {
		val targetOwner = owners.getValue(root.project(targetPath))
		var ancestor = owners.getValue(project).parent
		while (ancestor != null) {
			if (ancestor == targetOwner) return true
			ancestor = ancestor.parent
		}

		return false
	}

	private fun sharedApiCycles(): List<String> {
		val visited = mutableSetOf<String>()
		val active = mutableListOf<String>()
		val violations = mutableListOf<String>()
		fun visit(path: String) {
			val start = active.indexOf(path)
			if (start >= 0) {
				violations.add("sharedApis must not form a cycle: ${(active.drop(start) + path).joinToString(" -> ")}")
				return
			}
			if (!visited.add(path)) return
			val descriptor = descriptors[path] ?: return
			if (descriptor.kind != ArchitectureKind.API) return
			active.add(path)
			descriptor.sharedApis.sorted().forEach(::visit)
			active.removeAt(active.lastIndex)
		}
		descriptors.keys.forEach(::visit)

		return violations
	}

	private fun family(project: Project): String {
		extensions[project]?.family?.takeIf(String::isNotBlank)?.let { return it }
		if (shared.getValue(project)) return project.path

		val owner = owners.getValue(project)
		return extensions[owner]?.family?.takeIf(String::isNotBlank) ?: owner.path
	}

	private fun owner(project: Project): Project {
		if (kinds[project] == ArchitectureKind.API)
			return project.parent?.takeUnless { it == root } ?: project

		var owner: Project? = project
		while (owner != null && owner != root) {
			if (ownsApi(owner)) return owner
			owner = owner.parent
		}

		return project
	}

	private fun ownsApi(project: Project): Boolean =
		project.childProjects.values.any { kinds[it] == ArchitectureKind.API && shared[it] != true }

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

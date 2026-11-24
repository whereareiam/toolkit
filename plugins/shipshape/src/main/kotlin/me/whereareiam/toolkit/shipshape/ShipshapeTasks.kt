package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.architecture.ArchitectureKinds
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.verification.DefaultShipshapeExtension
import me.whereareiam.toolkit.shipshape.verification.task.ParsedSources
import me.whereareiam.toolkit.shipshape.verification.task.VerifyCategory
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Registers a task on the root project for every category a rule lies in, `verifyImports` for `imports`, as the
 * category appears, and makes each a part of `check`. Every task reads the Java's shape from one parse of the build.
 */
internal class ShipshapeTasks(
	private val project: Project,
	private val extension: DefaultShipshapeExtension,
) {
	private val parsedSources: Provider<ParsedSources> =
		project.gradle.sharedServices.registerIfAbsent(ParsedSources.NAME, ParsedSources::class.java) {
			parameters.javaRelease.set(extension.javaRelease)
		}

	fun register() = extension.registry.eachCategory(::register)

	private fun register(category: RuleCategory) {
		val task = project.tasks.register(category.taskName, VerifyCategory::class.java) {
			val rules = project.provider { extension.registry.rulesOf(category) }

			group = LifecycleBasePlugin.VERIFICATION_GROUP
			description = category.description
			this.category.set(category)
			this.rules.set(rules)
			modules.set(project.provider(this@ShipshapeTasks::modules))
			packageRoots.set(extension.packageRoots)
			wiringAnnotations.set(extension.wiringAnnotations)
			conventionPlugins.set(extension.conventionPlugins)
			exemptions.set(rules.map { extension.registry.declared(it.flatMap(Rule::exemptions).toSet()) })
			buildFile.set(project.buildFile.relativeTo(project.rootDir).invariantSeparatorsPath)
			javaRelease.set(extension.javaRelease)
			parser.set(Runtime.version().feature())
			sources.from(rules.map(this@ShipshapeTasks::sourceFiles))
			buildFiles.from(rules.map(this@ShipshapeTasks::buildFiles))
			parsedSources.set(this@ShipshapeTasks.parsedSources)
			usesService(this@ShipshapeTasks.parsedSources)
			directory.set(project.layout.projectDirectory)
			report.set(project.layout.buildDirectory.file("verify-${category.id}/report.txt"))
		}

		project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
			dependsOn(task)
		}
	}

	/** The Java files, where a rule reads the Java, and the files beside them its source rules name. */
	private fun sourceFiles(rules: List<Rule>): FileCollection {
		val readsJava = rules.any { it is ShapeRule || it is SourceRule }
		val java = if (readsJava) setOf("**/${JavaSources.PATTERN}") else emptySet()

		return read(java + rules.filterIsInstance<SourceRule>().flatMap(SourceRule::reads))
	}

	/** Every build file of the build, where a rule reads build files. */
	private fun buildFiles(rules: List<Rule>): FileCollection =
		read(if (rules.any { it is BuildRule }) setOf("**/${BuildFile.NAME}") else emptySet())

	/** Every project of the build, each with its architecture kind. */
	private fun modules(): Map<JavaModule, ArchitectureKind> = project.allprojects.sortedBy(Project::getPath).associate {
		JavaModule(it.path, it.projectDir.relativeTo(project.rootDir).invariantSeparatorsPath, it.name) to ArchitectureKinds.of(it)
	}

	/** The files [patterns] name below the root project's folder, without what a build left behind; none for no pattern. */
	private fun read(patterns: Set<String>): FileCollection {
		if (patterns.isEmpty()) return project.files()

		return project.fileTree(project.layout.projectDirectory) {
			include(patterns)
			exclude(BuildFile.LEFT_BEHIND.map { "**/$it/**" })
		}
	}
}

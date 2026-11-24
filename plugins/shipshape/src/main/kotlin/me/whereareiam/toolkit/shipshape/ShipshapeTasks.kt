package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.architecture.ArchitectureKinds
import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.verification.DefaultShipshapeExtension
import me.whereareiam.toolkit.shipshape.verification.StandardVerification
import me.whereareiam.toolkit.shipshape.verification.task.VerifyBuild
import me.whereareiam.toolkit.shipshape.verification.task.VerifyShape
import me.whereareiam.toolkit.shipshape.verification.task.VerifySource
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.language.base.plugins.LifecycleBasePlugin

/** Registers the three tasks of shipshape on the root project and makes each a part of `check`. */
internal class ShipshapeTasks(
	private val project: Project,
	private val extension: DefaultShipshapeExtension,
) {
	fun register() {
		val registry = extension.registry
		val shape = project.tasks.register(VerificationTask.SHAPE.taskName, VerifyShape::class.java) {
			configure(this, VerificationTask.SHAPE, registry::shapeRules)
			rules.set(project.provider(registry::shapeRules))
			javaRelease.set(extension.javaRelease)
			parser.set(Runtime.version().feature())
			sources.from(javaFiles())
		}
		val source = project.tasks.register(VerificationTask.SOURCE.taskName, VerifySource::class.java) {
			configure(this, VerificationTask.SOURCE, registry::sourceRules)
			rules.set(project.provider(registry::sourceRules))
			sources.from(javaFiles(), project.provider { read(registry.sourceRules().flatMap { it.reads }.toSet()) })
		}
		val build = project.tasks.register(VerificationTask.BUILD.taskName, VerifyBuild::class.java) {
			configure(this, VerificationTask.BUILD, registry::buildRules)
			rules.set(project.provider(registry::buildRules))
			conventionPlugins.set(extension.conventionPlugins)
			buildFiles.from(project.fileTree(project.layout.projectDirectory) {
				include("**/${BuildFile.NAME}")
				exclude(BuildFile.LEFT_BEHIND.map { "**/$it/**" })
			})
		}

		project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
			dependsOn(shape, source, build)
		}
	}

	private fun configure(task: StandardVerification, verification: VerificationTask, rules: () -> List<Rule>) {
		task.group = LifecycleBasePlugin.VERIFICATION_GROUP
		task.description = verification.description
		task.modules.set(project.provider(::modules))
		task.packageRoots.set(extension.packageRoots)
		task.wiringAnnotations.set(extension.wiringAnnotations)
		task.exemptions.set(project.provider { extension.registry.declared(rules().flatMap(Rule::exemptions).toSet()) })
		task.buildFile.set(project.buildFile.relativeTo(project.rootDir).invariantSeparatorsPath)
		task.directory.set(project.layout.projectDirectory)
		task.report.set(project.layout.buildDirectory.file("${verification.reportFolder}/report.txt"))
	}

	/** Every project of the build, each with its architecture kind. */
	private fun modules(): Map<JavaModule, ArchitectureKind> = project.allprojects.sortedBy(Project::getPath).associate {
		JavaModule(it.path, it.projectDir.relativeTo(project.rootDir).invariantSeparatorsPath, it.name) to ArchitectureKinds.of(it)
	}

	/** The Java files of every module, as [JavaSources] reads them, without what a build left behind. */
	private fun javaFiles(): FileCollection = read(setOf("**/${JavaSources.PATTERN}"))

	/** The files [patterns] name below the root project's folder, without what a build left behind; none for no pattern. */
	private fun read(patterns: Set<String>): FileCollection {
		if (patterns.isEmpty()) return project.files()

		return project.fileTree(project.layout.projectDirectory) {
			include(patterns)
			exclude(BuildFile.LEFT_BEHIND.map { "**/$it/**" })
		}
	}
}

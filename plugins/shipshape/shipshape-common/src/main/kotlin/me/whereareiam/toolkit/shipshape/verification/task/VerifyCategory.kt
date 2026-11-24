package me.whereareiam.toolkit.shipshape.verification.task

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.api.exemption.DeclaredExemptions
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclaration
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.report.BreachReport
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Holds the build to the rules of one category: each rule reads what it reads, the Java's shape, its text or the build
 * files, and the breaches are held against what the root build file excuses of the kinds the rules read. The rest
 * fails the build or warns, as each rule's enforcement says, and the task writes its report.
 */
@DisableCachingByDefault(because = "Reading the sources is as fast as fetching a result")
abstract class VerifyCategory : DefaultTask() {
	/** The category whose rules the task runs. */
	@get:Input
	abstract val category: Property<RuleCategory>

	/** The category's rules, held by the configuration cache; [ruleSet] is what tells a changed set. */
	@get:Internal
	abstract val rules: ListProperty<Rule>

	/** Every project of the build, the root among them, with its architecture kind. */
	@get:Input
	abstract val modules: MapProperty<JavaModule, ArchitectureKind>

	/** The packages the build's own types lie in or below. */
	@get:Input
	abstract val packageRoots: SetProperty<String>

	/** The simple names of the annotations that mark a wiring method. */
	@get:Input
	abstract val wiringAnnotations: SetProperty<String>

	/** The id namespaces of the build's own convention plugins, which the rules of build files read. */
	@get:Input
	abstract val conventionPlugins: SetProperty<String>

	/** What the root build file declares of the exemption kinds the rules read. */
	@get:Input
	abstract val exemptions: ListProperty<ExemptionDeclaration>

	/** The root build file by its path from the root project's folder, where an exemption that excuses nothing is placed. */
	@get:Input
	abstract val buildFile: Property<String>

	/** The language level the Java sources are written in, which the parser reads them at. */
	@get:Input
	@get:Optional
	abstract val javaRelease: Property<Int>

	/** The Java release that runs Gradle, whose parser reads the shape of the classes. */
	@get:Input
	abstract val parser: Property<Int>

	/** The Java sources and the other files the rules read, by their paths. */
	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	@get:IgnoreEmptyDirectories
	abstract val sources: ConfigurableFileCollection

	/** The build files of the build, where the category's rules read them. */
	@get:InputFiles
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val buildFiles: ConfigurableFileCollection

	/** The Java sources as the parser read them, once per build. */
	@get:Internal
	abstract val parsedSources: Property<ParsedSources>

	/** The root project's folder, which every path is taken from. Its content is tracked by [sources] and [buildFiles]. */
	@get:Internal
	abstract val directory: DirectoryProperty

	/** Every breach that was found, with the count of each rule, so that an unchanged build is not read twice. */
	@get:OutputFile
	abstract val report: RegularFileProperty

	/** Each rule by its id, its enforcement and its class, so that a changed set of rules runs the task again. */
	@get:Input
	val ruleSet: List<String>
		get() = rules.get().map { "${it.id} ${it.enforcement} ${it.javaClass.name}" }

	@TaskAction
	fun verify() {
		val context = context()
		val rules = rules.get()
		val found = shape(rules.filterIsInstance<ShapeRule>(), context) +
			source(rules.filterIsInstance<SourceRule>(), context) +
			build(rules.filterIsInstance<BuildRule>(), context)
		val kinds = rules.flatMap(Rule::exemptions).toSet()
		val findings = DeclaredExemptions(exemptions.get(), buildFile.get()).excuse(found, kinds)

		BreachReport(findings).conclude(name, report.get().asFile, logger, "The rules of ${category.get()} were violated")
	}

	private fun shape(rules: List<ShapeRule>, context: RuleContext): List<Pair<Rule, Breach>> {
		if (rules.isEmpty()) return emptyList()

		val parsing = parsedSources.get()
		val obstacle = parsing.obstacle
		if (obstacle != null) {
			logger.warn(
				"$name reads no shape of a class: Gradle runs on Java ${parser.get()}, which $obstacle. " +
					"Run Gradle on a JDK that can to have the shape of the classes read.",
			)
			return emptyList()
		}

		return parsing.of(javaSources()).flatMap { file -> rules.flatMap { rule -> rule.check(file, context).map { rule to it } } }
	}

	private fun source(rules: List<SourceRule>, context: RuleContext): List<Pair<Rule, Breach>> {
		if (rules.isEmpty()) return emptyList()

		val sources = javaSources()

		return rules.flatMap { rule -> rule.check(sources, context).map { rule to it } }
	}

	private fun build(rules: List<BuildRule>, context: RuleContext): List<Pair<Rule, Breach>> {
		if (rules.isEmpty()) return emptyList()

		return BuildFile.below(directory.get().asFile, conventionPlugins.get()).flatMap { file ->
			rules.flatMap { rule -> rule.check(file, context).map { rule to it } }.sortedBy { (_, breach) -> breach.line }
		}
	}

	private fun javaSources(): JavaSources = JavaSources(directory.get().asFile, modules.get().keys.sortedBy(JavaModule::path))

	private fun context(): RuleContext {
		val roots = packageRoots.get()
		if (roots.isEmpty())
			throw GradleException("$name needs the packages the build's own types lie in: set shipshape { packageRoots } or the group")

		return RuleContext(modules.get(), roots, wiringAnnotations.get())
	}
}

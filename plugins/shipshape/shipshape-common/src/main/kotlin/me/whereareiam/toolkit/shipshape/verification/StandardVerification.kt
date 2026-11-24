package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.architecture.type.ArchitectureKind
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.exemption.DeclaredExemptions
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclaration
import me.whereareiam.toolkit.shipshape.api.java.JavaModule
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.report.BreachReport
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.work.DisableCachingByDefault

/**
 * What the three tasks of shipshape share: their rules, what the rules may know of the build, what the build excuses of
 * them, and the report each writes.
 * A rule's breaches are held against the declarations of the kinds it reads, and the rest fails the build or warns, as
 * the rule's enforcement says.
 */
@DisableCachingByDefault(because = "Reading the sources is as fast as fetching a result")
abstract class StandardVerification : DefaultTask() {
	/** Every project of the build, the root among them, with its architecture kind. */
	@get:Input
	abstract val modules: MapProperty<JavaModule, ArchitectureKind>

	/** The packages the build's own types lie in or below. */
	@get:Input
	abstract val packageRoots: SetProperty<String>

	/** The simple names of the annotations that mark a wiring method. */
	@get:Input
	abstract val wiringAnnotations: SetProperty<String>

	/** What the root build file declares of the exemption kinds this task's rules read. */
	@get:Input
	abstract val exemptions: ListProperty<ExemptionDeclaration>

	/** The root build file by its path from the root project's folder, where an exemption that excuses nothing is placed. */
	@get:Input
	abstract val buildFile: Property<String>

	/** The root project's folder, which every path is taken from. Its content is tracked by each task's files. */
	@get:Internal
	abstract val directory: DirectoryProperty

	/** Every breach that was found, with the count of each rule, so that an unchanged build is not read twice. */
	@get:OutputFile
	abstract val report: RegularFileProperty

	/** Each rule this task runs by its id, its enforcement and its class, so that a changed set of rules runs the task again. */
	@get:Input
	val ruleSet: List<String>
		get() = checkedRules().map { "${it.id} ${it.enforcement} ${it.javaClass.name}" }

	/** The rules this task runs. */
	protected abstract fun checkedRules(): List<Rule>

	/**
	 * What the rules may know beside what they read.
	 *
	 * @throws GradleException when the build names no package root, which the rules tell its own types by
	 */
	protected fun context(): RuleContext {
		val roots = packageRoots.get()
		if (roots.isEmpty())
			throw GradleException("$name needs the packages the build's own types lie in: set shipshape { packageRoots } or the group")

		return RuleContext(modules.get(), roots, wiringAnnotations.get())
	}

	/**
	 * Ends the task: takes out what the declarations excuse, writes the report, warns and fails as the rules say.
	 *
	 * @param found what the rules found, each breach with the rule that found it
	 */
	protected fun conclude(task: VerificationTask, found: List<Pair<Rule, Breach>>) {
		val kinds = checkedRules().flatMap(Rule::exemptions).toSet()
		val findings = DeclaredExemptions(exemptions.get(), buildFile.get()).excuse(found, kinds)

		BreachReport(findings).conclude(name, report.get().asFile, logger, task.broken)
	}
}

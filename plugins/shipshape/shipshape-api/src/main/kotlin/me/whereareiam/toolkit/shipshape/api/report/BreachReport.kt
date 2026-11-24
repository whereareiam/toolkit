package me.whereareiam.toolkit.shipshape.api.report

import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File

/** What one task found, sorted into what fails the build and what is only reported. */
class BreachReport(findings: List<Finding>) {
	private val findings = findings.sortedWith(
		compareBy({ it.rule }, { it.breach.path }, { it.breach.line }, { it.breach.asks }),
	)

	/** The breaches of the rules that fail a build. */
	val failures: List<Finding> = this.findings.filter { it.enforcement == Enforcement.FAILS }

	/** The breaches of the rules that warn. */
	val warnings: List<Finding> = this.findings.filter { it.enforcement == Enforcement.WARNS }

	/** Every breach on a line of its own and then the count of each rule: what a task writes as its report. */
	fun text(): String =
		if (findings.isEmpty()) "The standard is kept.\n"
		else findings.joinToString("\n", postfix = "\n") { it.sentence } + counts(findings) + "\n"

	/**
	 * What a build prints of the rules that warn.
	 *
	 * @param task the task that found the breaches, `verifyImports`
	 * @return each breach with its place and the count of each rule, or `null` when every such rule is kept
	 */
	fun warning(task: String): String? {
		if (warnings.isEmpty()) return null

		return "$task: the standard is not kept in ${places(warnings.size)}. The build goes on: these rules warn, so that they " +
			"stop no delivery; repair the places.\n" + lines(warnings) + "\n${counts(warnings)}"
	}

	/**
	 * Ends a task: writes every breach to [file], prints through [logger] what the rules that warn found, and fails
	 * for the rest.
	 *
	 * @param task the task that found the breaches, `verifyImports`
	 * @param broken what the failures broke, as the first words of the failure: `The rules of the source were violated`
	 * @throws GradleException when a breach is of a rule that fails; it names every such breach
	 */
	fun conclude(task: String, file: File, logger: Logger, broken: String) {
		file.parentFile.mkdirs()
		file.writeText(text())
		warning(task)?.let(logger::warn)
		if (failures.isNotEmpty()) throw GradleException("$broken:\n" + lines(failures))
	}

	private companion object {
		/** [findings] as the lines of a failure, each naming its rule, its place and what is asked. */
		fun lines(findings: List<Finding>): String = findings.joinToString("\n") { " - ${it.sentence}" }

		/** How often each rule of the standard is broken in [findings], by its code, and how many places that is. */
		fun counts(findings: List<Finding>): String {
			val byRule = findings.groupingBy { it.rule }.eachCount().entries.joinToString(", ") { (rule, count) -> "$rule $count" }

			return "$byRule: ${places(findings.size)}"
		}

		fun places(count: Int): String = if (count == 1) "1 place" else "$count places"
	}
}

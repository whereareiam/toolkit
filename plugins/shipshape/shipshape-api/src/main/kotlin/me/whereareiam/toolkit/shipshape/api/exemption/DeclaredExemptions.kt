package me.whereareiam.toolkit.shipshape.api.exemption

import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.report.Finding
import me.whereareiam.toolkit.shipshape.api.type.Enforcement
import org.gradle.api.GradleException

/**
 * Holds what a task's rules found against what one build file declares as excused ([ExemptionDeclarations]). It takes
 * out what the declarations excuse, and makes a breach of each declaration that excuses nothing. Shipshape's tasks hold
 * the root build file's declarations so; a plugin's own task holds the declarations of the build file it reads, as a
 * module's own block declares them.
 */
class DeclaredExemptions(
	private val declarations: List<ExemptionDeclaration>,
	/** The build file the declarations are written in, by its path from the root project's folder. */
	private val buildFile: String,
) {
	/**
	 * Takes out of [found] what the declarations excuse.
	 *
	 * @param found what a task's rules found, each breach with the rule that found it
	 * @param kinds the kinds of exemption the task's rules read; a declaration of another kind is another task's
	 * @return the findings no declaration excuses, then one per declaration of [kinds] that excuses nothing
	 * @throws GradleException when a rule marks a breach with a kind it does not read
	 */
	fun excuse(found: List<Pair<Rule, Breach>>, kinds: Set<ExemptionKind>): List<Finding> {
		found.forEach { (rule, breach) -> readable(rule, breach) }

		val declared = declarations.filter { it.kind in kinds }
		val places = declared.map(ExemptionDeclaration::exemption).toSet()
		val excused = found.filter { (_, breach) -> breach.exemption in places }
		val used = excused.mapNotNull { (_, breach) -> breach.exemption }.toSet()
		val unused = declared.filter { it.exemption !in used }
			.sortedWith(compareBy({ it.kind.method }, ExemptionDeclaration::place))
			.map(::unused)

		return (found - excused.toSet()).map { (rule, breach) -> Finding(rule, breach) } + unused
	}

	private fun readable(rule: Rule, breach: Breach) {
		val kind = breach.exemption?.kind ?: return
		if (kind !in rule.exemptions)
			throw GradleException("${rule.id} marks a breach with the exemption kind ${kind.method}, which it does not read")
	}

	private fun unused(declaration: ExemptionDeclaration): Finding = Finding(
		UNUSED, Enforcement.FAILS,
		Breach(buildFile, null, "declares ${declaration.written}, and nothing there needs it; remove the declaration"),
	)

	companion object {
		/** The rule an exemption that excuses nothing breaks. */
		const val UNUSED = "unused-exemption"
	}
}

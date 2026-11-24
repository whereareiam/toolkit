package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.exemption.DeclaredExemptions
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclaration
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclarations
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import me.whereareiam.toolkit.shipshape.verification.type.VerificationTask
import org.gradle.api.GradleException

/**
 * Every rule and exemption kind a build registers, built in or through the `shipshape` block, and the places it
 * excuses. It refuses a second rule of one name, a second kind of one method, and a declaration of a kind nobody
 * registered.
 */
class RuleRegistry {
	private val shape = mutableListOf<ShapeRule>()
	private val source = mutableListOf<SourceRule>()
	private val build = mutableListOf<BuildRule>()
	private val kinds = linkedMapOf<String, ExemptionKind>()
	private val declarations = ExemptionDeclarations()

	fun register(rule: ShapeRule) {
		refuseTaken(rule)
		shape += rule
	}

	fun register(rule: SourceRule) {
		refuseTaken(rule)
		source += rule
	}

	fun register(rule: BuildRule) {
		refuseTaken(rule)
		build += rule
	}

	fun register(kind: ExemptionKind) {
		if (kind.method in kinds) throw GradleException("shipshape already has the exemption kind ${kind.method}")

		kinds[kind.method] = kind
	}

	/**
	 * Declares [place] excused from the rules that read [kind], as written in [block].
	 *
	 * @throws GradleException when the kind is not registered, or [ExemptionDeclarations.declare] refuses the declaration
	 */
	fun declare(kind: ExemptionKind, place: String, reason: String, block: String) {
		if (kinds[kind.method] != kind) throw GradleException("shipshape has no exemption kind ${kind.method}; register it first")

		declarations.declare(kind, place, reason, block)
	}

	/** The shape rules, once every rule reads only registered kinds and each kind is read by the rules of one task. */
	fun shapeRules(): List<ShapeRule> = checked().let { shape.toList() }

	/** The source rules, checked as [shapeRules] are. */
	fun sourceRules(): List<SourceRule> = checked().let { source.toList() }

	/** The build rules, checked as [shapeRules] are. */
	fun buildRules(): List<BuildRule> = checked().let { build.toList() }

	/** The declarations of [kinds], by the kind's method and then by the place. */
	fun declared(kinds: Set<ExemptionKind>): List<ExemptionDeclaration> = declarations.of(kinds)

	private fun refuseTaken(rule: Rule) {
		if (!ID.matches(rule.id)) throw GradleException("A rule's id is written as wildcard-import, not '${rule.id}'")
		if (rule.id == DeclaredExemptions.UNUSED) throw GradleException("shipshape names its own rule ${rule.id}")

		val taken = (shape + source + build).firstOrNull { it.id == rule.id } ?: return

		throw GradleException("shipshape already has a rule ${rule.id}: ${taken.javaClass.name}")
	}

	/** Holds the registered rules against the registered kinds: each read kind is registered and read by one task's rules. */
	private fun checked() {
		val readers = readers()
		val unregistered = readers.keys.firstOrNull { kinds[it.method] != it }
		if (unregistered != null)
			throw GradleException("A rule reads the exemption kind ${unregistered.method}, which is not registered with shipshape")

		val shared = readers.entries.firstOrNull { it.value.size > 1 }
		if (shared != null) {
			val tasks = shared.value.sorted().joinToString(" and ") { it.taskName }

			throw GradleException("The exemption kind ${shared.key.method} is read by $tasks; a kind belongs to the rules of one task")
		}

		val unread = kinds.values.firstOrNull { it !in readers }
		if (unread != null)
			throw GradleException("No rule reads the exemption kind ${unread.method}; a kind is registered with the rule that reads it")
	}

	/** Every kind a rule reads, with the tasks whose rules read it. */
	private fun readers(): Map<ExemptionKind, Set<VerificationTask>> {
		val families = mapOf(VerificationTask.SHAPE to shape, VerificationTask.SOURCE to source, VerificationTask.BUILD to build)

		return families.flatMap { (task, rules) -> rules.flatMap(Rule::exemptions).map { it to task } }
			.groupBy({ it.first }, { it.second })
			.mapValues { (_, tasks) -> tasks.toSet() }
	}

	private companion object {
		val ID = Regex("[a-z][a-z0-9]*(-[a-z0-9]+)*")
	}
}

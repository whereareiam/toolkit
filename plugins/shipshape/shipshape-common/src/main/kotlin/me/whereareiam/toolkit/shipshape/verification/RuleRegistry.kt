package me.whereareiam.toolkit.shipshape.verification

import me.whereareiam.toolkit.shipshape.api.BuildRule
import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.api.RuleCategory
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.exemption.DeclaredExemptions
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclaration
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionDeclarations
import me.whereareiam.toolkit.shipshape.api.exemption.ExemptionKind
import org.gradle.api.GradleException

/**
 * Every rule and exemption kind a build registers, built in or through the `shipshape` block, the categories the rules
 * lie in, and the places the build excuses. It refuses a second rule of one id, two categories of one id that say
 * different things, a second kind of one method, and a declaration of a kind nobody registered.
 */
class RuleRegistry {
	private val rules = mutableListOf<Rule>()
	private val categories = linkedMapOf<String, RuleCategory>()
	private val categoryActions = mutableListOf<(RuleCategory) -> Unit>()
	private val kinds = linkedMapOf<String, ExemptionKind>()
	private val declarations = ExemptionDeclarations()

	/** Adds a rule that reads the shape of the Java classes. */
	fun register(rule: ShapeRule) = add(rule)

	/** Adds a rule that reads the Java source as text. */
	fun register(rule: SourceRule) = add(rule)

	/** Adds a rule that reads the build files. */
	fun register(rule: BuildRule) = add(rule)

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

	/** Runs [action] for every category a rule lies in, those registered already and each that a later rule brings. */
	fun eachCategory(action: (RuleCategory) -> Unit) {
		categoryActions += action
		categories.values.toList().forEach(action)
	}

	/** The rules of [category], once every rule reads only registered kinds and each kind is read by one category's rules. */
	fun rulesOf(category: RuleCategory): List<Rule> = checked().let { rules.filter { it.category == category } }

	/** Every rule, checked as [rulesOf] is. */
	fun rules(): List<Rule> = checked().let { rules.toList() }

	/** The declarations of [kinds], by the kind's method and then by the place. */
	fun declared(kinds: Set<ExemptionKind>): List<ExemptionDeclaration> = declarations.of(kinds)

	private fun add(rule: Rule) {
		if (!ID.matches(rule.id)) throw GradleException("A rule's id is written as wildcard-import, not '${rule.id}'")
		if (rule.id == DeclaredExemptions.UNUSED) throw GradleException("shipshape names its own rule ${rule.id}")

		val taken = rules.firstOrNull { it.id == rule.id }
		if (taken != null) throw GradleException("shipshape already has a rule ${rule.id}: ${taken.javaClass.name}")

		rules += rule
		addCategory(rule.category)
	}

	private fun addCategory(category: RuleCategory) {
		val known = categories[category.id]
		if (known == category) return
		if (known != null)
			throw GradleException("The category ${category.id} is '${known.description}' already, not '${category.description}'")

		categories[category.id] = category
		categoryActions.toList().forEach { it(category) }
	}

	/** Holds the registered rules against the registered kinds: each read kind is registered and read by one category's rules. */
	private fun checked() {
		val readers = readers()
		val unregistered = readers.keys.firstOrNull { kinds[it.method] != it }
		if (unregistered != null)
			throw GradleException("A rule reads the exemption kind ${unregistered.method}, which is not registered with shipshape")

		val shared = readers.entries.firstOrNull { it.value.size > 1 }
		if (shared != null) {
			val read = shared.value.map(RuleCategory::id).sorted().joinToString(" and ")

			throw GradleException("The exemption kind ${shared.key.method} is read by the rules of $read; a kind belongs to one category")
		}

		val unread = kinds.values.firstOrNull { it !in readers }
		if (unread != null)
			throw GradleException("No rule reads the exemption kind ${unread.method}; a kind is registered with the rule that reads it")
	}

	/** Every kind a rule reads, with the categories whose rules read it. */
	private fun readers(): Map<ExemptionKind, Set<RuleCategory>> =
		rules.flatMap { rule -> rule.exemptions.map { it to rule.category } }
			.groupBy({ it.first }, { it.second })
			.mapValues { (_, categories) -> categories.toSet() }

	private companion object {
		val ID = Regex("[a-z][a-z0-9]*(-[a-z0-9]+)*")
	}
}

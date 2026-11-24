package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A method or a constructor of main code takes at most five parameters. More are an options object or a
 * command model. A method that overrides another takes what that one takes, and is held there; a wiring method, one
 * that carries an annotation of `shipshape { wiringAnnotations }`, takes collaborators, which `collaborators` holds.
 */
internal class ParameterRule : ShapeRule {
	override val id = "parameters"
	override val category = SizeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val skipped = OVERRIDE + context.wiringAnnotations

		return parsed.methods.filter { (_, method) -> method.parameters.size > LIMIT && !ParsedSource.annotated(method.modifiers, skipped) }
			.map { (type, method) ->
				val named = if (ParsedSource.isConstructor(method)) "the constructor of ${type.simpleName}" else "${method.name}()"

				Breach(
					parsed.source.path, parsed.lineOf(method),
					"declares $named with ${method.parameters.size} parameters; a method takes at most $LIMIT, more are one model",
				)
			}
	}

	internal companion object {
		/** How many parameters a method takes at most. */
		const val LIMIT = 5

		/** What a method that takes what another type declares carries. */
		val OVERRIDE = setOf("Override")
	}
}

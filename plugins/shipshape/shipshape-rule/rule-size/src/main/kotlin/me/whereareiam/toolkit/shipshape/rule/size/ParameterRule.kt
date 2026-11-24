package me.whereareiam.toolkit.shipshape.rule.size

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.ShapeRule
import me.whereareiam.toolkit.shipshape.api.java.ParsedSource
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A method or a constructor of main code takes at most five parameters. More are an options object or a
 * command model. A method that overrides another takes what that one takes, and is held there. An injector handler of
 * Mixin or MixinExtras, one annotated `@Inject`, `@WrapOperation` and the like, takes what its target and the injector
 * give it, which no model of the build's own can hold; its annotation is read by its full name, so `jakarta.inject.Inject`
 * is not one. A wiring method, one that carries an annotation of `shipshape { wiringAnnotations }`, takes
 * collaborators, which `collaborators` holds.
 */
internal class ParameterRule : ShapeRule {
	override val id = "parameters"
	override val category = SizeRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(parsed: ParsedSource, context: RuleContext): List<Breach> {
		if (!parsed.source.main) return emptyList()

		val skipped = OVERRIDE + context.wiringAnnotations

		return parsed.methods.filter { (_, method) -> method.parameters.size > LIMIT }
			.filterNot { (_, method) -> ParsedSource.annotated(method.modifiers, skipped) }
			.filterNot { (_, method) -> parsed.annotatedAs(method.modifiers, INJECTORS) }
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

		/**
		 * The injectors of Mixin and MixinExtras, by full name, whose handlers take what the injector gives them.
		 * MixinExtras keeps `WrapWithCondition` in two packages, the first deprecated.
		 */
		val INJECTORS = setOf(
			"org.spongepowered.asm.mixin.injection.Inject",
			"org.spongepowered.asm.mixin.injection.Redirect",
			"org.spongepowered.asm.mixin.injection.ModifyArg",
			"org.spongepowered.asm.mixin.injection.ModifyArgs",
			"org.spongepowered.asm.mixin.injection.ModifyVariable",
			"org.spongepowered.asm.mixin.injection.ModifyConstant",
			"com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation",
			"com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod",
			"com.llamalad7.mixinextras.injector.WrapWithCondition",
			"com.llamalad7.mixinextras.injector.v2.WrapWithCondition",
			"com.llamalad7.mixinextras.injector.ModifyExpressionValue",
			"com.llamalad7.mixinextras.injector.ModifyReturnValue",
			"com.llamalad7.mixinextras.injector.ModifyReceiver",
		)
	}
}

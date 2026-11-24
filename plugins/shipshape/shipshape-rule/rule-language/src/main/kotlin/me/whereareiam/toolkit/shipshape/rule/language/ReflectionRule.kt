package me.whereareiam.toolkit.shipshape.rule.language

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * Main code reflects on nothing and makes no dynamic proxy. It finds no class by its name (`Class.forName`), looks
 * up no member (any `getDeclared…`, `getConstructor` and `getConstructors`, and `getMethod` and `getField` with the
 * member's name), opens no member (`setAccessible`), makes no proxy (`Proxy.newProxyInstance`) and takes no lookup for method handles
 * (`MethodHandles.lookup`, `publicLookup`, `privateLookupIn`). Code that routes calls by
 * scanning them, or reaches past a boundary, works around a missing owner: the choice is made explicit, and a library
 * that lacks a capability is fixed.
 *
 * A call is what the rule reads, never a type's name: a library may hand a `java.lang.reflect.Method` to its callers,
 * as JUnit hands an extension the test method, and taking it and reading its annotations reflects on nothing. Reading
 * text, the rule leaves out the calls an ordinary getter also writes, `getFields()` and `getMethods()` without a name.
 */
internal class ReflectionRule : SourceRule {
	override val id = "reflection"
	override val category = LanguageRules.CATEGORY

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filter(JavaSource::main).flatMap { source ->
		FORMS.flatMap { (form, does) ->
			source.find(form).map { Breach(source.path, it.line, "${does(it.group(1))}; $ASKED") }
		}.sortedBy(Breach::line)
	}

	private companion object {
		const val ASKED = "own code reflects on nothing; make the choice explicit, and fix the library that lacks the capability"

		/** The forms of reflection, each with what a place of it does. */
		val FORMS: List<Pair<Regex, (String) -> String>> = listOf(
			Regex("""\b(Proxy\s*\.\s*newProxyInstance|Class\s*\.\s*forName|MethodHandles\s*\.\s*\w*[lL]ookup\w*)\s*\(""") to
				{ call: String -> "calls ${call.filterNot(Char::isWhitespace)}" },
			Regex("""\.\s*(getDeclared\w*|getConstructors?|setAccessible)\s*\(""") to { call: String -> "calls $call" },
			Regex("""\.\s*(get(?:Method|Field))\s*\(\s*"""") to { call: String -> "calls $call" },
		)
	}
}

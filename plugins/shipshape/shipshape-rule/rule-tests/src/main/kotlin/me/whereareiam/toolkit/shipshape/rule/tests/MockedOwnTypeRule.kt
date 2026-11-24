package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A contract of the build's own is tested against a hand-written fake, which is a class a reader can open.
 * Mockito stands in for a foreign type only, one whose fake nobody would write (`HttpServletRequest`). A type is the
 * build's own when it lies below one of its package roots, or in the package of the test.
 */
internal class MockedOwnTypeRule : SourceRule {
	override val id = "mocked-own-type"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.all.filterNot { it.main }.flatMap { source ->
		ownMocks(source, sources.typesOf(source.packageName), context)
	}

	/**
	 * The mocks of [source] that stand in for a type of the build's own: one it names in full or imports from below a
	 * package root, or one of its own package, whose names are [neighbours].
	 */
	private fun ownMocks(source: JavaSource, neighbours: Set<String>, context: RuleContext): List<Breach> {
		val imported = source.imports.associateBy { it.substringAfterLast('.') }

		return source.find(MOCK).filter { mock ->
			val type = mock.group(1)
			val outermost = type.substringBefore('.')
			val ofThePackage = outermost !in imported && outermost in neighbours

			context.isOwn(type) || imported[outermost]?.let(context::isOwn) == true || ofThePackage
		}.map {
			Breach(
				source.path, it.line,
				"mocks ${it.group(1)}, a type of the build's own; write a fake of it, Mockito stands in for a foreign type only",
			)
		}
	}

	private companion object {
		val MOCK = Regex("""\b(?:mock|spy)\s*\(\s*([\w.]+)\.class\b""")
	}
}

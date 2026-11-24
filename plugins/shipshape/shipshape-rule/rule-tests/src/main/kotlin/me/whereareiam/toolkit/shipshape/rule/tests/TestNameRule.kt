package me.whereareiam.toolkit.shipshape.rule.tests

import me.whereareiam.toolkit.shipshape.api.RuleContext
import me.whereareiam.toolkit.shipshape.api.SourceRule
import me.whereareiam.toolkit.shipshape.api.java.JavaSource
import me.whereareiam.toolkit.shipshape.api.java.JavaSources
import me.whereareiam.toolkit.shipshape.api.report.Breach
import me.whereareiam.toolkit.shipshape.api.type.Enforcement

/**
 * A test class is named after a class of the same package in the same module, `<Class>Test` or
 * `<Class>IntegrationTest`, and the second name and the tag `integration` go together. A file of the source set
 * `test` that holds tests is read.
 */
internal class TestNameRule : SourceRule {
	override val id = "test-name"

	override val enforcement = Enforcement.FAILS

	override fun check(sources: JavaSources, context: RuleContext): List<Breach> = sources.modules.flatMap { module ->
		sources.of(module).filter { it.isOf(JavaSource.TEST) }.flatMap { misnamed(it, module.label) }
	}

	/**
	 * What the file of a test, [source], breaks by its name and its tag.
	 *
	 * @param module how a breach names the module the file lies in
	 */
	private fun misnamed(source: JavaSource, module: String): List<Breach> {
		val tagged = TAG.containsMatchIn(source.text)
		val named = source.typeName.endsWith(TEST)
		if (!named && !tagged && !TEST_METHOD.containsMatchIn(source.code)) return emptyList()

		val integration = source.typeName.endsWith(INTEGRATION_TEST)
		val covered = covered(source, named, integration)
		val name = when {
			!named -> "holds tests and is not named <Class>$TEST or <Class>$INTEGRATION_TEST"
			covered.isEmpty() -> "is named after no class of ${source.packageName.ifEmpty { "<none>" }} in $module"
			else -> null
		}

		return buildList {
			name?.let(::add)
			if (tagged && !integration) add("is tagged $INTEGRATION_TAG and is not named <Class>$INTEGRATION_TEST")
			// FooIntegrationTest may be the plain test of a class FooIntegration; then it needs no tag.
			if (integration && !tagged && source.typeName.removeSuffix(TEST) !in covered)
				add("is named <Class>$INTEGRATION_TEST and does not carry @Tag(\"$INTEGRATION_TAG\")")
		}.map { Breach(source.path, null, it) }
	}

	/** The classes of the package of [source] in its module's main code that the test's name names. */
	private fun covered(source: JavaSource, named: Boolean, integration: Boolean): List<String> = buildList {
		if (integration) add(source.typeName.removeSuffix(INTEGRATION_TEST))
		if (named) add(source.typeName.removeSuffix(TEST))
	}.filter(String::isNotEmpty).filter { source.siblingFolder(JavaSource.MAIN).resolve("$it.java").isFile }

	private companion object {
		const val TEST = "Test"
		const val INTEGRATION_TEST = "IntegrationTest"

		/** The tag of a test that needs what runs outside its JVM. */
		const val INTEGRATION_TAG = "integration"

		val TAG = Regex("""@Tag\(\s*"$INTEGRATION_TAG"\s*\)""")
		val TEST_METHOD = Regex("""@(Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)(?!\w)""")
	}
}

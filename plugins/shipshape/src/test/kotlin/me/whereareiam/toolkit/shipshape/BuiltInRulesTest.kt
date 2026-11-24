package me.whereareiam.toolkit.shipshape

import me.whereareiam.toolkit.shipshape.api.Rule
import me.whereareiam.toolkit.shipshape.verification.DefaultShipshapeExtension
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.File

class BuiltInRulesTest {
	@Test
	fun `shipshape holds the general rules, each under a name of its own`() {
		val registry = registered().registry

		assertThat(registry.shapeRules()).hasSize(22)
		assertThat(registry.sourceRules()).hasSize(21)
		assertThat(registry.buildRules()).hasSize(3)
		assertThat(rules().map(Rule::id)).doesNotHaveDuplicates()
	}

	@Test
	fun `every rule has a test of its own, beside it in its subject's tests`() {
		assertThat(rules().map { test(it) }).allSatisfy {
			assertThat(File(it)).describedAs(it).isFile()
		}
	}

	private fun rules(): List<Rule> = registered().registry.let { it.shapeRules() + it.sourceRules() + it.buildRules() }

	private fun registered(): DefaultShipshapeExtension {
		val extension = ProjectBuilder.builder().build().objects.newInstance(DefaultShipshapeExtension::class.java)
		BuiltInRules.registerWith(extension)

		return extension
	}

	/** The test of [rule] by its path from this module: in the tests of the subject module whose package it lies in. */
	private fun test(rule: Rule): String {
		val type = rule.javaClass.name
		val subject = type.substringAfter(".rule.").substringBefore('.')

		return "shipshape-rule/rule-$subject/src/test/kotlin/${type.replace('.', '/')}Test.kt"
	}
}

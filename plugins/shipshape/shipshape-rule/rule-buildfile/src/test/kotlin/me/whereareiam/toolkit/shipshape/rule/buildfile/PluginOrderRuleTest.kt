package me.whereareiam.toolkit.shipshape.rule.buildfile

import me.whereareiam.toolkit.shipshape.api.buildfile.BuildFile
import me.whereareiam.toolkit.shipshape.verification.LayoutFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PluginOrderRuleTest {
	private val rule = PluginOrderRule()

	@Test
	fun `built-ins, external ids, then conventions pass, each kind by the length of the name and then by the alphabet`() {
		val file = BuildFile("build.gradle.kts", """
			plugins {
				base
				`maven-publish`
				`java-test-fixtures`
				id("build.buf")
				id("org.openapi.generator")
				id("permission-catalogue")
				id("com.example.convention.api")
				id("com.example.convention.adapter")
				id("com.example.convention.library")
				id("com.example.convention.integration")
			}
		""".trimIndent(), CONVENTIONS)

		assertThat(LayoutFixture.breaches(rule, file)).isEmpty()
	}

	@Test
	fun `a kind behind a later one, a later letter before an earlier and a longer name before a shorter are named`() {
		val file = BuildFile("error-response-api/build.gradle.kts", """
			plugins {
				id("com.example.convention.api")
				`java-test-fixtures`
				id("com.example.convention.test")
				id("com.example.convention.java")
				id("problem-catalogue")
			}
		""".trimIndent(), CONVENTIONS)

		assertThat(LayoutFixture.breaches(rule, file)).containsExactly(
			"plugin-order error-response-api/build.gradle.kts:3 applies java-test-fixtures behind com.example.convention.api; $ASKED",
			"plugin-order error-response-api/build.gradle.kts:5 applies com.example.convention.java behind com.example.convention.test; " +
				"$ASKED",
			"plugin-order error-response-api/build.gradle.kts:6 applies problem-catalogue behind com.example.convention.java; $ASKED",
		)
	}

	private companion object {
		/** The namespace of the conventions of the builds the tests read. */
		val CONVENTIONS = setOf("com.example.convention")

		const val ASKED = "plugins stand as built-ins, external ids, then conventions, " +
			"each kind by the length of the name and then by the alphabet"
	}
}
